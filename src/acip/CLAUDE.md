# ACIP Methods — All 10 Administrative Methods

All ACIP methods require a valid ACIP license on the AIR server.
All ACIP methods use the same transport as UCIP — same TCP socket, same XML-RPC format.

## How every method file is structured

```js
import { buildMethodCall } from '../core/xmlBuilder.js'
import { sendRequest }     from '../core/transport.js'
import { parseMethodResponse } from '../core/xmlParser.js'
import { toAirDate, generateTransactionId } from '../utils/dateUtils.js'
import { describeResponseCode } from '../utils/errorCodes.js'

/**
 * @param {MethodParams} params
 * @returns {Promise<AirResponse>}
 */
export default async function methodName(params) {
  // 1. Validate required fields (throw Error if missing)
  // 2. Build params object (common fields + method-specific fields)
  // 3. Build XML: buildMethodCall('MethodName', paramsObj)
  // 4. Send: sendRequest(xml, 'MethodName')
  // 5. Parse: parseMethodResponse(rawXml)
  // 6. Map response to AirResponse shape
  // 7. Return AirResponse
}
```

## Common params object builder (use in every method)

```js
function commonParams(subscriberNumber) {
  return {
    originNodeType:      process.env.AIR_NODE_TYPE ?? 'EXT',
    originHostName:      process.env.AIR_HOST_NAME ?? 'app',
    originTransactionID: generateTransactionId(),
    originTimeStamp:     toAirDate(new Date()),
    subscriberNumber,
  }
}
```

---

## A1 — installSubscriber.js

### Function signature
```js
export default async function installSubscriber({
  subscriberNumber,    // string, REQUIRED
  serviceClassNew,     // number, REQUIRED
  temporaryBlockedFlag = false,          // boolean, optional
  languageIDNew = 1,                     // number, optional
  ussdEndOfCallNotificationID = 255,     // number, optional
})
```

### Validation (throw before sending)
- `subscriberNumber` must be a non-empty string
- `serviceClassNew` must be a positive integer

### XML params to build
```js
{
  ...commonParams(subscriberNumber),
  serviceClassNew,
  temporaryBlockedFlag,          // boolean → <boolean>
  languageIDNew,                 // number → <int>
  ussdEndOfCallNotificationID,   // number → <int>
}
```

### Response data shape (data field on success)
```js
{
  // InstallSubscriber has minimal response data — just confirmation
  installed: true
}
```

### Special handling
- `responseCode 142` → `success: false`, message: "Subscriber already installed"
- This is a common idempotency case — callers may want to treat 142 as "already done"

---

## A2 — deleteSubscriber.js

### Function signature
```js
export default async function deleteSubscriber({
  subscriberNumber,      // string, REQUIRED
  originOperatorID,      // string, optional
})
```

### Validation
- `subscriberNumber` must be non-empty string

### XML params
```js
{
  ...commonParams(subscriberNumber),
  originOperatorID,   // omit if not provided
}
```

### Response data shape
```js
{ deleted: true }
```

### Special handling
- `responseCode 156` means has active subordinates — surface specific message:
  "Cannot delete: subscriber has active subordinate accounts. Delete subordinates first."

---

## A3 — linkSubordinateSubscriber.js

### Function signature
```js
export default async function linkSubordinateSubscriber({
  subscriberNumber,       // string, REQUIRED — the subordinate
  masterAccountNumber,    // string, REQUIRED — the master
  originOperatorID,       // string, optional
})
```

### Validation
- Both `subscriberNumber` and `masterAccountNumber` required
- They must be different strings

### XML params
```js
{
  ...commonParams(subscriberNumber),
  originOperatorID,
  masterAccountNumber,
}
```

### Response data shape
```js
{ linked: true, masterAccountNumber }
```

### Special handling
- Codes 143, 144, 145, 146 all indicate topology conflicts — map each to a specific
  `responseMessage` from `errorCodes.describeResponseCode(code).message`

---

## A4 — updateTemporaryBlocked.js

### Function signature
```js
export default async function updateTemporaryBlocked({
  subscriberNumber,   // string, REQUIRED
  blocked,            // boolean, REQUIRED (true=block, false=unblock)
})
```

### Validation
- `blocked` must be explicitly provided (boolean) — do not default

### XML params
```js
{
  ...commonParams(subscriberNumber),
  temporaryBlockedFlag: blocked,   // boolean → <boolean>
}
```

### Response data shape
```js
{ blocked: blocked }   // echo the new state
```

### Special handling
- `responseCode 152` on unblock → message: "Cannot unblock: account has expired"

---

## A5 — getPromotionCounters.js

### Function signature
```js
export default async function getPromotionCounters({
  subscriberNumber,   // string, REQUIRED
})
```

### XML params
```js
{ ...commonParams(subscriberNumber) }
```

### Response data shape
```js
{
  counters: [
    {
      id: number,                    // promotionCounterID
      value: string,                 // promotionCounterValue
      expiryDate: Date|null,         // promotionCounterExpiryDate → JS Date
      planProgressCounter: number,   // promotionPlanProgressCounter
    }
  ],
  refillAmountAccumulated: string,   // promotionRefillAmountAccumulated
}
```

### Parsing rules
- `promotionCounterInformation` may be absent → return `{ counters: [], refillAmountAccumulated: '0' }`
- If only one counter, AIR may return object instead of array — normalise to array

---

## A6 — updatePromotionCounters.js

### Function signature
```js
export default async function updatePromotionCounters({
  subscriberNumber,                   // string, REQUIRED
  transactionCurrency,                // string, conditional (required if amount given)
  promotionRefillAmountRelative,      // string, optional
})
```

### Validation
- If `promotionRefillAmountRelative` is provided, `transactionCurrency` MUST also be provided

### XML params
```js
{
  ...commonParams(subscriberNumber),
  transactionCurrency,
  promotionRefillAmountRelative,
}
```

### Response data shape
```js
{ updated: true }
```

---

## A7 — getPromotionPlans.js

### Function signature
```js
export default async function getPromotionPlans({
  subscriberNumber,    // string, REQUIRED
  originOperatorID,    // string, optional
})
```

### XML params
```js
{
  ...commonParams(subscriberNumber),
  originOperatorID,
}
```

### Response data shape
```js
{
  plans: [
    {
      id: number,             // promotionPlanID
      startDate: Date|null,   // promotionStartDate
      endDate: Date|null,     // promotionEndDate
    }
  ]
}
```

### Parsing rules
- `promotionPlanInformation` may be absent → `{ plans: [] }`
- Single plan → normalise object to array

---

## A8 — updatePromotionPlan.js

### Function signature
```js
export default async function updatePromotionPlan({
  subscriberNumber,   // string, REQUIRED
  action,             // 'ADD'|'SET'|'DELETE', REQUIRED
  planId,             // string, mandatory for ADD, optional for SET
  oldStartDate,       // Date|string, mandatory for SET/DELETE
  oldEndDate,         // Date|string, mandatory for SET/DELETE
  startDate,          // Date|string, mandatory for ADD/SET
  endDate,            // Date|string, mandatory for ADD/SET
})
```

### Validation (enforce action-specific rules)
```
ADD:    planId required, startDate required, endDate required
SET:    oldStartDate required, oldEndDate required, startDate required, endDate required
DELETE: oldStartDate required, oldEndDate required
```
Throw `Error('UpdatePromotionPlan ADD: startDate is required')` etc.

### XML params
```js
{
  ...commonParams(subscriberNumber),
  promotionPlanAction: action,
  promotionPlanID:         planId         ?? undefined,
  promotionOldStartDate:   toAirDate(oldStartDate) ?? undefined,
  promotionOldEndDate:     toAirDate(oldEndDate)   ?? undefined,
  promotionStartDate:      toAirDate(startDate)    ?? undefined,
  promotionEndDate:        toAirDate(endDate)       ?? undefined,
}
```
Omit undefined values — xmlBuilder skips null/undefined fields.

### Response data shape
```js
{ action, planId }
```

---

## A9 — updateAccumulators.js

### Function signature
```js
export default async function updateAccumulators({
  subscriberNumber,       // string, REQUIRED
  accumulators,           // AccumulatorEntry[], REQUIRED, min length 1
  serviceClassCurrent,    // number, optional
  originOperatorID,       // string, optional
})
```

### Validation
- `accumulators` must be non-empty array
- Each entry must have `accumulatorId`
- Each entry must have EXACTLY ONE of `relativeValue` or `absoluteValue`
- If multiple entries use `relativeValue`, ALL must have same sign (all positive or all negative)

### XML params
```js
{
  ...commonParams(subscriberNumber),
  originOperatorID,
  serviceClassCurrent,
  accumulatorInformation: accumulators.map(a => ({
    accumulatorID:            a.accumulatorId,
    accumulatorValueRelative: a.relativeValue   ?? undefined,
    accumulatorValueAbsolute: a.absoluteValue   ?? undefined,
    accumulatorStartDate:     a.startDate ? toAirDate(a.startDate) : undefined,
  }))
}
```

### Response data shape
```js
{ updated: true, count: accumulators.length }
```

---

## A10 — updateRefillBarring.js

### Function signature
```js
export default async function updateRefillBarring({
  subscriberNumber,   // string, REQUIRED
  action,             // 'BAR'|'CLEAR'|'STEP', REQUIRED
})
```

### Validation
- `action` must be one of `['BAR', 'CLEAR', 'STEP']`

### XML params
```js
{
  ...commonParams(subscriberNumber),
  refillBarAction: action,
}
```

### Response data shape
```js
{ action, applied: true }
```
