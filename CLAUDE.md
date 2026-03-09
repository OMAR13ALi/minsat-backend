# UCIP/ACIP AIR-IP 3.0 — API Wrapper Project

## What This Project Is

A Node.js wrapper library that exposes all ACIP and UCIP AIR-IP 3.0 methods as clean
async JavaScript functions. The wrapper handles XML building, HTTP transport, response
parsing, and error normalisation so callers never touch raw XML.

## Project Structure

```
ucip-wrapper/
├── CLAUDE.md                  ← you are here — project overview & rules
├── src/
│   ├── core/
│   │   ├── CLAUDE.md          ← transport, XML builder, response parser
│   │   ├── transport.js       ← raw HTTP-over-TCP sender
│   │   ├── xmlBuilder.js      ← builds methodCall XML bodies
│   │   └── xmlParser.js       ← parses methodResponse XML into plain objects
│   ├── types/
│   │   ├── CLAUDE.md          ← all TypeScript/JSDoc types
│   │   └── index.js           ← exported type definitions (JSDoc)
│   ├── utils/
│   │   ├── CLAUDE.md          ← helpers: date formatting, currency, error codes
│   │   ├── dateUtils.js       ← ISO-8601 ↔ JS Date conversions
│   │   ├── currencyUtils.js   ← smallest-unit ↔ decimal conversions
│   │   └── errorCodes.js      ← full response code catalogue
│   ├── acip/
│   │   ├── CLAUDE.md          ← all 10 ACIP method implementations
│   │   ├── installSubscriber.js
│   │   ├── deleteSubscriber.js
│   │   ├── linkSubordinateSubscriber.js
│   │   ├── updateTemporaryBlocked.js
│   │   ├── getPromotionCounters.js
│   │   ├── updatePromotionCounters.js
│   │   ├── getPromotionPlans.js
│   │   ├── updatePromotionPlan.js
│   │   ├── updateAccumulators.js
│   │   └── updateRefillBarring.js
│   ├── ucip/
│   │   ├── CLAUDE.md          ← all 13 UCIP method implementations
│   │   ├── getAccumulators.js
│   │   ├── getBalanceAndDate.js
│   │   ├── updateBalanceAndDate.js
│   │   ├── getAccountDetails.js
│   │   ├── updateAccountDetails.js
│   │   ├── getAllowedServiceClasses.js
│   │   ├── updateServiceClass.js
│   │   ├── getFaFList.js
│   │   ├── updateFaFList.js
│   │   ├── getRefillOptions.js
│   │   ├── refill.js
│   │   ├── updateCommunityList.js
│   │   └── updateSubscriberSegmentation.js
│   └── index.js               ← public API — exports AcipClient and UcipClient
├── tests/
│   ├── core/
│   ├── acip/
│   └── ucip/
├── package.json
└── .env.example
```

## Technology Stack

- **Runtime**: Node.js 18+
- **HTTP transport**: Native `net` module (TCP socket to port 10010) — NOT axios/fetch
- **XML building**: `xmlbuilder2` — `npm install xmlbuilder2`
- **XML parsing**: `fast-xml-parser` — `npm install fast-xml-parser`
- **Testing**: `jest`
- **Types**: JSDoc (no TypeScript compilation required, but full JSDoc coverage expected)
- **Env config**: `dotenv`

## AIR Server Protocol — Critical Facts

### Transport
- All requests go to `AIR_HOST:10010` via **raw TCP socket** (not HTTP/1.1 keep-alive in the
  browser sense — send the full HTTP/1.1 request as text over a TCP socket using `net.Socket`)
- Protocol is HTTP/2.0 in the spec but AIR servers accept the text over TCP directly
- Each request is self-contained: open socket → send → read until `</methodResponse>` → close

### HTTP Header (mandatory for every request)
```
POST /Air HTTP/2.0
Accept: text/xml
Connection: keep-alive
Content-Length: <EXACT byte count of XML body>
Content-Type: text/xml
Date: <RFC-2822 date string>
Host: <AIR_HOST>
User-Agent: UGw Server/3.0/1.0
Authorization: Basic <base64(user:password)>
```
**Content-Length MUST be exact** — too low = truncated request, too high = server waits/times out.

### Authentication
- HTTP Basic Auth, Base64-encoded `user:password`
- Credentials come from environment variables `AIR_USER` and `AIR_PASSWORD`
- Authorization header: `Basic ${Buffer.from(`${user}:${password}`).toString('base64')}`

### XML Body Format
```xml
<?xml version="1.0"?>
<methodCall>
  <methodName>MethodName</methodName>
  <params>
    <param>
      <value>
        <struct>
          <member>
            <name>fieldName</name>
            <value><string>value</string></value>
          </member>
        </struct>
      </value>
    </param>
  </params>
</methodCall>
```
**Note**: The field tag in requests uses `<name>` but some AIR responses use `<n>` as shorthand.
The parser must handle both `<name>` and `<n>` as member name tags.

### Timestamps
- All timestamps in requests: ISO-8601 format `YYYYMMDDTHHmmss±HHMM`
  Example: `20240101T10:00:00+0000`
- All timestamps in responses: same format
- `originTimeStamp` = current UTC time at moment of request

### originTransactionID
- Must be unique per request
- Use: `Date.now().toString()` or a UUID — store it for correlation/logging

## Shared Request Fields (every single method)

Every method — both ACIP and UCIP — MUST include these 5 fields:

| Field                | Type   | Value                                      |
|----------------------|--------|--------------------------------------------|
| originNodeType       | string | From env: `AIR_NODE_TYPE` (default: "EXT") |
| originHostName       | string | From env: `AIR_HOST_NAME` (default: hostname)|
| originTransactionID  | string | Unique ID generated per request            |
| originTimeStamp      | dateTime | Current UTC time in AIR ISO-8601 format  |
| subscriberNumber     | string | MSISDN passed by the caller                |

## Environment Variables

```env
AIR_HOST=air1a-n5              # AIR server hostname or IP
AIR_PORT=10010                 # Default AIR port
AIR_USER=user                  # Basic auth username
AIR_PASSWORD=password          # Basic auth password
AIR_NODE_TYPE=EXT              # originNodeType sent in all requests
AIR_HOST_NAME=myapp            # originHostName sent in all requests
AIR_TIMEOUT_MS=10000           # Socket timeout in ms (default 10000)
CURRENCY_DECIMALS=2            # Decimal places for currency (2=cents, 3=millimes)
```

## Universal Return Shape

**Every wrapper function returns this shape — no exceptions:**

```js
{
  success: boolean,          // true only when responseCode === 0
  responseCode: number,      // raw code from AIR (0 = ok)
  responseMessage: string,   // human-readable status
  transactionId: string,     // the originTransactionID echoed back
  data: object | null,       // parsed response payload (null on error)
  raw: object                // the full parsed XML response as plain object
}
```

On network/timeout errors (not AIR errors), return:
```js
{
  success: false,
  responseCode: -1,
  responseMessage: 'Network error: <message>',
  transactionId: null,
  data: null,
  raw: null
}
```

## Error Handling Rules

1. **Never throw** — always return the standard shape
2. **responseCode !== 0** → `success: false`, populate `responseCode` and `responseMessage`
3. **Socket timeout** → `responseCode: -1`, `responseMessage: 'Timeout after Xms'`
4. **XML parse failure** → `responseCode: -2`, `responseMessage: 'XML parse error: ...'`
5. **Validation failure** (missing required field) → `responseCode: -3`, throw descriptive Error
   before sending (the only case where throwing is acceptable — pre-flight validation)

## Data Type Conventions

### Currency / Monetary values
- AIR stores money in the **smallest currency unit** (e.g. cents for TND)
- Wrapper exposes amounts as **integers in smallest unit** — conversion to decimals is the
  caller's responsibility using `currencyUtils.toDecimal(value, decimals)`
- Never auto-convert amounts in wrapper functions — pass them through as-is

### Dates
- Incoming to wrapper: **JS Date objects** or **ISO-8601 strings** (both accepted)
- Outgoing to AIR: AIR ISO-8601 format (use `dateUtils.toAirDate(date)`)
- Incoming from AIR: parse to **JS Date objects** in `data`, keep raw string in `raw`
- Use `dateUtils.fromAirDate(str)` to parse AIR timestamps

### Booleans
- AIR sends/receives booleans as `<boolean>0</boolean>` or `<boolean>1</boolean>`
- Wrapper maps `0 → false`, `1 → true` in responses
- Wrapper maps `false → 0`, `true → 1` in requests

### Arrays
- Empty arrays in responses → return `[]`, never `null`
- Missing array fields in response → return `[]`

## Coding Standards

- **ES modules** (`import`/`export`) — use `.js` extensions in imports
- **async/await** throughout — no callbacks, no raw Promise chains
- **JSDoc on every exported function** — include `@param`, `@returns`, `@throws`
- **No external HTTP clients** — only Node.js `net` module for transport
- **Validate required fields** before building XML — throw `Error` with field name
- **Log** every outgoing request and incoming response at DEBUG level using `console.debug`
  (gated by `process.env.AIR_DEBUG === 'true'`)
- One file per method — no grouping multiple methods in one file
- Export a single default function from each method file

## Testing Rules

- Mock the transport layer (`src/core/transport.js`) in all unit tests
- Test every response code variant for each method
- Test malformed XML responses
- Test timeout scenarios
- Test missing required field validation
- Use `jest` with `--coverage` — target 90%+ coverage

## Quick Reference: All Methods

### ACIP (src/acip/) — Administrative, all licensed
| File                          | AIR Method                  | Key params                                    |
|-------------------------------|-----------------------------|-----------------------------------------------|
| installSubscriber.js          | InstallSubscriber           | subscriberNumber, serviceClassNew             |
| deleteSubscriber.js           | DeleteSubscriber            | subscriberNumber                              |
| linkSubordinateSubscriber.js  | LinkSubordinateSubscriber   | subscriberNumber, masterAccountNumber         |
| updateTemporaryBlocked.js     | UpdateTemporaryBlocked      | subscriberNumber, blocked (boolean)           |
| getPromotionCounters.js       | GetPromotionCounters        | subscriberNumber                              |
| updatePromotionCounters.js    | UpdatePromotionCounters     | subscriberNumber, currency, amount            |
| getPromotionPlans.js          | GetPromotionPlans           | subscriberNumber                              |
| updatePromotionPlan.js        | UpdatePromotionPlan         | subscriberNumber, action, planId, dates       |
| updateAccumulators.js         | UpdateAccumulators          | subscriberNumber, accumulators[]              |
| updateRefillBarring.js        | UpdateRefillBarring         | subscriberNumber, action (BAR/CLEAR/STEP)     |

### UCIP (src/ucip/) — User-facing, some licensed
| File                          | AIR Method                  | Key params                                    |
|-------------------------------|-----------------------------|-----------------------------------------------|
| getAccumulators.js            | GetAccumulators             | subscriberNumber                              |
| getBalanceAndDate.js          | GetBalanceAndDate           | subscriberNumber                              |
| updateBalanceAndDate.js       | UpdateBalanceAndDate        | subscriberNumber, amount, currency, dates     |
| getAccountDetails.js          | GetAccountDetails           | subscriberNumber, includeLocation?            |
| updateAccountDetails.js       | UpdateAccountDetails        | subscriberNumber, eocnId                      |
| getAllowedServiceClasses.js   | GetAllowedServiceClasses    | subscriberNumber                              |
| updateServiceClass.js         | UpdateServiceClass          | subscriberNumber, action, serviceClassNew     |
| getFaFList.js                 | GetFaFList                  | subscriberNumber, requestedOwner?             |
| updateFaFList.js              | UpdateFaFList               | subscriberNumber, action, entries[]           |
| getRefillOptions.js           | GetRefillOptions            | subscriberNumber, voucherCode?, serviceClass? |
| refill.js                     | Refill                      | subscriberNumber, (amount+currency+profileId) OR voucherCode |
| updateCommunityList.js        | UpdateCommunityList         | subscriberNumber, communityIds[]              |
| updateSubscriberSegmentation.js| UpdateSubscriberSegmentation| subscriberNumber, accountGroupId             |

## See Also

- `src/core/CLAUDE.md` — transport and XML layer details
- `src/acip/CLAUDE.md` — ACIP method contracts and business rules
- `src/ucip/CLAUDE.md` — UCIP method contracts and business rules
- `src/utils/CLAUDE.md` — utility helpers
- `src/types/CLAUDE.md` — all type definitions
