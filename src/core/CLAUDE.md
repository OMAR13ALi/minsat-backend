# Core Layer — Transport, XML Builder, XML Parser

## Files in this directory

| File           | Responsibility                                               |
|----------------|--------------------------------------------------------------|
| transport.js   | Opens TCP socket, sends HTTP request, reads raw response     |
| xmlBuilder.js  | Builds the XML `<methodCall>` body from a plain JS object    |
| xmlParser.js   | Parses `<methodResponse>` XML into a plain JS object         |

---

## transport.js

### What it does
Opens a `net.Socket` to `AIR_HOST:AIR_PORT`, sends a complete HTTP request (header +
body as a single string), reads data until the socket closes or `</methodResponse>` is
detected, then resolves with the raw response string.

### Function signature
```js
/**
 * @param {string} xmlBody   - The complete XML methodCall body string
 * @param {string} methodName - Used only for debug logging
 * @returns {Promise<string>} - Raw HTTP response string (header + body)
 */
export async function sendRequest(xmlBody, methodName)
```

### Implementation rules
1. Calculate `Content-Length` as `Buffer.byteLength(xmlBody, 'utf8')` — NOT `xmlBody.length`
   (they differ for non-ASCII characters)
2. Build the full HTTP request string:
   ```
   POST /Air HTTP/2.0\r\n
   Accept: text/xml\r\n
   Connection: keep-alive\r\n
   Content-Length: {byteLength}\r\n
   Content-Type: text/xml\r\n
   Date: {new Date().toUTCString()}\r\n
   Host: {AIR_HOST}\r\n
   User-Agent: UGw Server/3.0/1.0\r\n
   Authorization: Basic {base64creds}\r\n
   \r\n
   {xmlBody}
   ```
3. Use `net.createConnection({ host, port, timeout: AIR_TIMEOUT_MS })`
4. Accumulate chunks in a Buffer — do NOT assume response comes in one chunk
5. Resolve when `</methodResponse>` is found in the accumulated data OR socket closes
6. Reject on `socket.on('error', ...)` and `socket.on('timeout', ...)`
7. Always call `socket.destroy()` in a finally block
8. Strip the HTTP header from the response — return only the XML body (everything after `\r\n\r\n`)

### Error codes returned (not thrown)
- Timeout → reject with `Error('Timeout after ${AIR_TIMEOUT_MS}ms')`
- Connection refused → reject with `Error('Connection refused: ${host}:${port}')`

---

## xmlBuilder.js

### What it does
Takes a method name and a plain JS object of parameters and produces a valid AIR
`<methodCall>` XML string.

### Function signature
```js
/**
 * @param {string} methodName - e.g. 'InstallSubscriber'
 * @param {object} params     - flat or nested parameter object
 * @returns {string}          - complete <?xml ...?><methodCall>...</methodCall> string
 */
export function buildMethodCall(methodName, params)
```

### Value type mapping (JS → XML)
| JS type   | Example value       | XML element                        |
|-----------|---------------------|------------------------------------|
| string    | `"PAG"`             | `<string>PAG</string>`             |
| number (int) | `201`            | `<int>201</int>`                   |
| boolean   | `true` / `false`    | `<boolean>1</boolean>` / `<boolean>0</boolean>` |
| Date      | `new Date()`        | `<dateTime.iso8601>20240101T...</dateTime.iso8601>` |
| string (ISO date) | `"20240101T..."` | `<dateTime.iso8601>...</dateTime.iso8601>` |
| Array     | `[{...}, {...}]`    | `<array><data><value>...</value></data></array>` |
| null/undefined | —            | skip the field entirely            |

### Date detection rule
A string value is treated as a dateTime if it matches: `/^\d{8}T\d{6}/`
Otherwise it is treated as a plain string.

### Array handling
Each array element that is an object becomes a `<struct>` inside `<value>`.
Each array element that is a primitive uses the type mapping above.

### Nested object handling
A nested plain object becomes a `<struct>` with its keys as members.

### Field name tag
Use `<n>` for the member name tag (matches AIR server expectation):
```xml
<member>
  <n>subscriberNumber</n>
  <value><string>9172139951</string></value>
</member>
```

### Example output
```js
buildMethodCall('InstallSubscriber', {
  originNodeType: 'EXT',
  subscriberNumber: '9172139951',
  serviceClassNew: 201,
  temporaryBlockedFlag: false,
})
```
Produces:
```xml
<?xml version="1.0"?>
<methodCall>
  <methodName>InstallSubscriber</methodName>
  <params>
    <param>
      <value>
        <struct>
          <member><n>originNodeType</n><value><string>EXT</string></value></member>
          <member><n>subscriberNumber</n><value><string>9172139951</string></value></member>
          <member><n>serviceClassNew</n><value><int>201</int></value></member>
          <member><n>temporaryBlockedFlag</n><value><boolean>0</boolean></value></member>
        </struct>
      </value>
    </param>
  </params>
</methodCall>
```

---

## xmlParser.js

### What it does
Parses a `<methodResponse>` XML string into a plain JS object with proper type coercion.

### Function signature
```js
/**
 * @param {string} xmlString - Raw XML methodResponse string
 * @returns {object}         - Parsed response as plain JS object
 * @throws {Error}           - If XML is malformed (caller catches and maps to responseCode -2)
 */
export function parseMethodResponse(xmlString)
```

### Type coercion rules (XML → JS)
| XML element          | JS type                                      |
|----------------------|----------------------------------------------|
| `<string>`           | string                                        |
| `<int>` / `<i4>`     | number (parseInt)                             |
| `<boolean>`          | boolean (`"0"` → false, `"1"` → true)         |
| `<double>`           | number (parseFloat)                           |
| `<dateTime.iso8601>` | JS Date object                               |
| `<array><data>`      | Array — each `<value>` becomes array element  |
| `<struct>`           | Plain object — each `<member>` becomes a key  |
| `<nil>`              | null                                          |

### Member name tag
The parser must handle BOTH `<n>` and `<name>` as the member name element.

### Fault responses
If the response contains `<fault>` instead of `<params>`, return:
```js
{
  responseCode: faultCode (int),
  responseMessage: faultString (string)
}
```

### Empty/missing fields
- Missing field → `undefined` (not null — lets callers use `??` operator)
- Empty `<array>` → `[]`
- Empty `<struct>` → `{}`

### Parser library usage
Use `fast-xml-parser` with these options:
```js
{
  ignoreAttributes: false,
  parseAttributeValue: true,
  parseTagValue: true,
  trimValues: true,
}
```
Post-process the parsed tree with a recursive walk to apply the type coercion above.

---

## Shared header builder

Both ACIP and UCIP callers use the same header builder. Put it in `transport.js`:

```js
/**
 * @param {number} contentLength - byte length of the XML body
 * @returns {string} - HTTP header block ending with \r\n\r\n
 */
function buildHttpHeader(contentLength)
```

---

## Debug logging

Gate all logging behind `process.env.AIR_DEBUG === 'true'`:
```js
function debug(label, data) {
  if (process.env.AIR_DEBUG === 'true') {
    console.debug(`[AIR ${new Date().toISOString()}] ${label}`, data ?? '')
  }
}
```

Log points:
- Before send: `[AIR ...] → MethodName txnId=...`
- After receive: `[AIR ...] ← MethodName responseCode=... ms=...`
- On error: `[AIR ...] ✗ MethodName error=...`
