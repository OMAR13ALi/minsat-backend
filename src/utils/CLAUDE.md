# Utils — Date, Currency, Error Codes

## dateUtils.js

### toAirDate(input)
Converts a JS Date or ISO-8601 string to AIR's timestamp format.

```js
/**
 * @param {Date|string} input
 * @returns {string} AIR-format timestamp: 'YYYYMMDDTHHmmss+0000'
 */
export function toAirDate(input)
```

**Rules:**
- Always output UTC: `+0000` suffix
- Format: `YYYYMMDDTHHmmss+0000` — no dashes, no colons, T separator
- If input is already in AIR format (`/^\d{8}T\d{6}/`), return as-is
- If input is a JS Date, use `toISOString()` then reformat
- If input is null/undefined, return `toAirDate(new Date())`

```js
toAirDate(new Date('2024-01-15T10:30:00Z'))  // '20240115T103000+0000'
toAirDate('2024-01-15T10:30:00.000Z')         // '20240115T103000+0000'
toAirDate('20240115T103000+0000')             // '20240115T103000+0000' (passthrough)
```

### fromAirDate(str)
Parses an AIR timestamp string to a JS Date.

```js
/**
 * @param {string|null|undefined} str
 * @returns {Date|null}
 */
export function fromAirDate(str)
```

**Rules:**
- Input format: `YYYYMMDDTHHmmss±HHMM`
- Returns `null` for null/undefined/empty input
- Returns `null` for unparseable strings (log a debug warning)
- Handles both `+0000` and `-0500` timezone offsets

### generateTransactionId()
```js
/**
 * @returns {string} Unique transaction ID
 */
export function generateTransactionId()
// Implementation: `Date.now().toString(36).toUpperCase() + '-' + Math.random().toString(36).slice(2,7).toUpperCase()`
// Example output: 'LR2KX8A-F4B2C'
```

---

## currencyUtils.js

### toDecimal(smallestUnit, decimals)
```js
/**
 * Converts AIR's smallest-unit integer string to a decimal string.
 * @param {string|number} smallestUnit  - e.g. '25000'
 * @param {number}        decimals      - decimal places (2=cents, 3=millimes)
 * @returns {string}                    - e.g. '250.00'
 */
export function toDecimal(smallestUnit, decimals = 2)
```
```js
toDecimal('25000', 2)   // '250.00'
toDecimal('25000', 3)   // '25.000'
toDecimal('1000', 2)    // '10.00'
```

### toSmallestUnit(decimal, decimals)
```js
/**
 * Converts a decimal amount string to AIR's smallest-unit integer string.
 * @param {string|number} decimal
 * @param {number}        decimals
 * @returns {string}
 */
export function toSmallestUnit(decimal, decimals = 2)
```
```js
toSmallestUnit('10.00', 2)  // '1000'
toSmallestUnit(10.5, 2)     // '1050'
```

**Note:** These utilities are provided for caller convenience. The wrapper itself
**never auto-converts** — it passes amounts through exactly as received.

---

## errorCodes.js

Full catalogue of every AIR response code.

```js
/**
 * @param {number} code
 * @returns {{ message: string, category: 'success'|'subscriber'|'account'|'voucher'|'plan'|'system' }}
 */
export function describeResponseCode(code)
```

### Code catalogue

| Code | Message                                      | Category   |
|------|----------------------------------------------|------------|
| 0    | Request succeeded                            | success    |
| -1   | Network error                                | system     |
| -2   | XML parse error                              | system     |
| -3   | Validation error                             | system     |
| 100  | Other error — verify AF configuration        | system     |
| 102  | Subscriber not found                         | subscriber |
| 103  | Account barred for refill                    | account    |
| 104  | Temporary blocked                            | account    |
| 105  | Dedicated account not allowed                | account    |
| 106  | Dedicated account negative balance           | account    |
| 107  | Voucher already used by same subscriber      | voucher    |
| 108  | Voucher already used by different subscriber | voucher    |
| 109  | Voucher status unavailable                   | voucher    |
| 110  | Voucher expired                              | voucher    |
| 111  | Voucher stolen or missing                    | voucher    |
| 112  | Voucher damaged                              | voucher    |
| 113  | Voucher status pending                       | voucher    |
| 114  | Voucher type not accepted                    | voucher    |
| 115  | Voucher group service class error            | voucher    |
| 117  | Service class change not allowed             | account    |
| 119  | Invalid voucher activation code              | voucher    |
| 120  | Invalid refill profile                       | account    |
| 121  | Supervision period too long                  | account    |
| 122  | Service fee period too long                  | account    |
| 123  | Max credit limit exceeded                    | account    |
| 124  | Below minimum balance                        | account    |
| 126  | Account not active                           | account    |
| 127  | Accumulator not available                    | account    |
| 128  | Invalid PIN code                             | account    |
| 130  | FaF number not allowed                       | account    |
| 133  | Service class list empty                     | account    |
| 134  | Accumulator overflow                         | account    |
| 135  | Accumulator underflow                        | account    |
| 136  | Date adjustment error                        | account    |
| 137  | GetBalanceAndDate not allowed                | account    |
| 138  | No PIN code registered                       | account    |
| 139  | Dedicated account not defined                | account    |
| 140  | Invalid old service class                    | account    |
| 141  | Invalid language                             | account    |
| 142  | Subscriber already installed                 | subscriber |
| 143  | Invalid master subscriber                    | subscriber |
| 144  | Subscriber already activated                 | subscriber |
| 145  | Already linked as subordinate                | subscriber |
| 146  | Already linked as master                     | subscriber |
| 147  | Invalid old community list                   | account    |
| 148  | Invalid new community list                   | account    |
| 149  | Invalid promotion plan end date              | plan       |
| 150  | Invalid promotion plan ID                    | plan       |
| 151  | Promotion plan not found                     | plan       |
| 152  | Deblocking of expired account                | account    |
| 153  | Dedicated account max credit limit exceeded  | account    |
| 154  | Invalid old service class date               | account    |
| 155  | Invalid new service class                    | account    |
| 156  | Delete failed                                | subscriber |
| 157  | Invalid account home region                  | account    |
| 158  | Max promotion plans already reached          | plan       |

### isRetryable(code)
```js
/**
 * Returns true for transient errors worth retrying.
 * @param {number} code
 * @returns {boolean}
 */
export function isRetryable(code)
// Retryable: -1 (network), 100 (other error)
// NOT retryable: everything else (subscriber state errors)
```
