# UCIP Methods — All 13 User Interface Protocol Methods

UCIP methods handle subscriber-facing operations: balances, refills, bundles, FaF,
service class. Some are licensed (marked below). All use the same transport layer
as ACIP methods.

## How every method file is structured

```js
import { buildMethodCall }      from '../core/xmlBuilder.js'
import { sendRequest }          from '../core/transport.js'
import { parseMethodResponse }  from '../core/xmlParser.js'
import { toAirDate, fromAirDate, generateTransactionId } from '../utils/dateUtils.js'
import { describeResponseCode } from '../utils/errorCodes.js'

/**
 * @param {MethodParams} params
 * @returns {Promise<import('../types').AirResponse>}
 */
export default async function methodName(params) {
  // 1. Validate required fields (throw Error if missing)
  // 2. Build params object (common fields + method-specific)
  // 3. Build XML via buildMethodCall('MethodName', paramsObj)
  // 4. Send via sendRequest(xml, 'MethodName')
  // 5. Parse via parseMethodResponse(rawXml)
  // 6. Map to AirResponse and return
}
```

## Common params builder (same as ACIP — repeat in every method file)

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

## U1 — getAccumulators.js

**Purpose:** Read all accumulator values for a subscriber. Accumulators track usage
(data bytes, voice minutes, SMS count) within a period.

### Function signature
```js
export default async function getAccumulators({
  subscriberNumber,   // string, REQUIRED
})
```

### XML params
```js
{ ...commonParams(subscriberNumber) }
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 104  | Temporary blocked |
| 126  | Account not active |
| 127  | Accumulator not available |

### Response data shape (`data` field on success)
```js
{
  accumulators: [
    {
      id:          number,    // accumulatorID
      value:       string,    // accumulatorValue — in accumulator's native unit
      startDate:   Date|null, // accumulatorStartDate → JS Date
      expiryDate:  Date|null, // accumulatorExpiryDate → JS Date
      threshold:   string,    // accumulatorThreshold — max allowed value
    }
  ]
}
```

### Parsing rules
- `accumulatorInformation` absent → `{ accumulators: [] }`
- Single accumulator may come as object, not array — normalise to `[item]`
- All date fields: use `fromAirDate()` to parse

---

## U2 — getBalanceAndDate.js

**Purpose:** The most fundamental read. Returns main balance + all lifecycle dates +
dedicated account balances. Call this to show the subscriber's current financial state.

### Function signature
```js
export default async function getBalanceAndDate({
  subscriberNumber,   // string, REQUIRED
})
```

### XML params
```js
{ ...commonParams(subscriberNumber) }
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 123  | Max credit limit exceeded |
| 124  | Below minimum balance |
| 126  | Account not active |
| 137  | GetBalanceAndDate not allowed |

### Response data shape
```js
{
  accountValue:          string,    // main balance in smallest currency unit
  supervisionExpiryDate: Date|null,
  serviceFeeExpiryDate:  Date|null,
  creditClearancePeriod: number,    // days
  serviceRemovalPeriod:  number,    // days
  temporaryBlocked:      boolean,   // temporaryBlockedFlag 0→false 1→true
  serviceClassCurrent:   number,
  languageId:            number,    // languageIDCurrent
  eocnId:                number,    // ussdEndOfCallNotificationID
  dedicatedAccounts: [
    {
      id:         number,   // dedicatedAccountID
      value:      string,   // dedicatedAccountValue
      expiryDate: Date|null // dedicatedAccountExpiryDate
    }
  ]
}
```

### Parsing rules
- `dedicatedAccountInformation` absent → `dedicatedAccounts: []`
- Single dedicated account may come as object — normalise to array
- `temporaryBlockedFlag`: coerce `0 → false`, `1 → true`

---

## U3 — updateBalanceAndDate.js

**Purpose:** The primary balance adjustment method. Top-ups, deductions, expiry date
changes, dedicated account updates — all in one call.

### Function signature
```js
export default async function updateBalanceAndDate({
  subscriberNumber,         // string, REQUIRED
  currency,                 // string, conditional (REQUIRED if any balance is changed)
  adjustmentAmount,         // string, optional — main balance relative change (±)
  supervisionExpiryDate,    // Date|string, optional
  serviceFeeExpiryDate,     // Date|string, optional
  creditClearancePeriod,    // number, optional — days
  serviceRemovalPeriod,     // number, optional — days
  dedicatedAccounts,        // DedicatedAccountUpdate[], optional
})
```

### Validation
- If `adjustmentAmount` is provided → `currency` MUST be provided
- If any `dedicatedAccounts` entry has `relativeAmount` or `absoluteValue` → `currency` MUST be provided
- Each `dedicatedAccounts` entry must have `accountId`
- Each entry must have EXACTLY ONE of `relativeAmount` or `absoluteValue`

### XML params
```js
{
  ...commonParams(subscriberNumber),
  transactionCurrency:         currency          ?? undefined,
  adjustmentAmountRelative:    adjustmentAmount  ?? undefined,
  supervisionExpiryDate:       supervisionExpiryDate ? toAirDate(supervisionExpiryDate) : undefined,
  serviceFeeExpiryDate:        serviceFeeExpiryDate  ? toAirDate(serviceFeeExpiryDate)  : undefined,
  creditClearancePeriod:       creditClearancePeriod ?? undefined,
  serviceRemovalPeriod:        serviceRemovalPeriod  ?? undefined,
  dedicatedAccountUpdateInformation: dedicatedAccounts?.length ? dedicatedAccounts.map(d => ({
    dedicatedAccountID:         d.accountId,
    adjustmentAmountRelative:   d.relativeAmount  ?? undefined,
    dedicatedAccountValueNew:   d.absoluteValue   ?? undefined,
  })) : undefined,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 104  | Temporary blocked |
| 105  | Dedicated account not allowed |
| 106  | Dedicated account would go negative |
| 121  | Supervision period too long |
| 122  | Service fee period too long |
| 123  | Max credit limit exceeded |
| 124  | Below minimum balance |
| 126  | Account not active |
| 136  | Date adjustment error |
| 139  | Dedicated account not defined |
| 153  | Dedicated account max credit limit exceeded |

### Response data shape
```js
{
  accountValueAfter: string|null,   // new main balance (when returned by server)
  dedicatedAccounts: [
    {
      id:    number,
      value: string,  // new balance after update
    }
  ]
}
```

### Parsing rules
- `accountValueAfter` may be absent on some AIR versions — return `null` if missing
- `dedicatedAccountInformation` absent → `dedicatedAccounts: []`

---

## U4 — getAccountDetails.js

**Purpose:** Full subscriber profile. Pass `includeLocation: true` to also get network
location data (current VLR, MSC, roaming status).

### Function signature
```js
export default async function getAccountDetails({
  subscriberNumber,      // string, REQUIRED
  includeLocation,       // boolean, optional (default: false)
})
```

### XML params
```js
{
  ...commonParams(subscriberNumber),
  requestedInformationFlags: includeLocation ? {
    requestLocationInformationFlag: true   // → <boolean>1</boolean>
  } : undefined,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |

### Response data shape
```js
{
  serviceClassCurrent:   number,
  temporaryBlocked:      boolean,
  supervisionExpiryDate: Date|null,
  serviceFeeExpiryDate:  Date|null,
  languageId:            number,
  eocnId:                number,
  accountHomeRegion:     string,
  location: {             // null if includeLocation was false or server didn't return it
    currentVLR:   string,
    currentMSC:   string,
    roaming:      boolean,  // roamingFlag
    timestamp:    Date|null // locationTimestamp
  } | null,
  communityIds: number[]  // from communityInformation[].communityID
}
```

### Parsing rules
- `locationInformation` absent → `location: null`
- `communityInformation` absent → `communityIds: []`
- Single community → normalise to array

---

## U5 — updateAccountDetails.js

**Purpose:** Update subscriber account settings — primarily the USSD end-of-call
notification ID (EOCN) that controls what message subscribers hear after calls.

### Function signature
```js
export default async function updateAccountDetails({
  subscriberNumber,             // string, REQUIRED
  ussdEndOfCallNotificationId,  // number, optional — 0-199 per tariff tree
})
```

### Validation
- At least one optional field must be provided

### XML params
```js
{
  ...commonParams(subscriberNumber),
  ussdEndOfCallNotificationID: ussdEndOfCallNotificationId ?? undefined,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 104  | Temporary blocked |
| 128  | Invalid PIN code |
| 138  | No PIN code registered |
| 141  | Invalid language |
| 157  | Invalid account home region |

### Response data shape
```js
{ updated: true }
```

---

## U6 — getAllowedServiceClasses.js *(Licensed)*

**Purpose:** Returns which service classes (plans) this subscriber is eligible to
switch to. Always call this before showing a plan upgrade UI.

### Function signature
```js
export default async function getAllowedServiceClasses({
  subscriberNumber,   // string, REQUIRED
})
```

### XML params
```js
{ ...commonParams(subscriberNumber) }
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 133  | Service class list is empty |

### Response data shape
```js
{
  serviceClasses: number[]   // array of allowed serviceClassID values
}
```

### Parsing rules
- `serviceClassInformation` absent OR code 133 → `{ serviceClasses: [] }`
- Single entry → normalise to array
- Extract `serviceClassID` from each struct entry

---

## U7 — updateServiceClass.js *(Licensed)*

**Purpose:** Change a subscriber's plan (service class). SetOriginal = permanent change.
SetTemporary = override until DeleteTemporary is called.

### Function signature
```js
export default async function updateServiceClass({
  subscriberNumber,    // string, REQUIRED
  action,              // 'SetOriginal'|'SetTemporary'|'DeleteTemporary', REQUIRED
  serviceClassNew,     // number, conditional (required for SetOriginal/SetTemporary)
})
```

### Validation
- `action` must be one of `['SetOriginal', 'SetTemporary', 'DeleteTemporary']`
- `serviceClassNew` required when `action !== 'DeleteTemporary'`

### XML params
```js
{
  ...commonParams(subscriberNumber),
  serviceClassAction: action,
  serviceClassNew:    serviceClassNew ?? undefined,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 104  | Temporary blocked |
| 117  | Service class change not allowed |
| 126  | Account not active |
| 140  | Invalid old service class |
| 154  | Invalid old service class date |
| 155  | Invalid new service class |

### Response data shape
```js
{
  action,
  serviceClassCurrent: number,   // new active service class after change
}
```

---

## U8 — getFaFList.js *(Licensed)*

**Purpose:** Get the subscriber's Friends and Family list — MSISDNs that receive
preferential call rates.

### Function signature
```js
export default async function getFaFList({
  subscriberNumber,    // string, REQUIRED
  requestedOwner,      // number, optional — 2 = subscriber-owned entries only
})
```

### XML params
```js
{
  ...commonParams(subscriberNumber),
  requestedOwner: requestedOwner ?? undefined,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 126  | Account not active |

### Response data shape
```js
{
  entries: [
    {
      fafNumber: string,  // MSISDN
      owner:     string,  // 'Subscriber' or 'Operator'
    }
  ]
}
```

### Parsing rules
- `fafInformation` absent → `{ entries: [] }`
- Single entry → normalise to array

---

## U9 — updateFaFList.js *(Licensed)*

**Purpose:** Add, replace, or remove Friends and Family entries.

**CRITICAL:** `SET` replaces the ENTIRE list. Always pass the full desired list, never
just the delta, when using SET.

### Function signature
```js
export default async function updateFaFList({
  subscriberNumber,   // string, REQUIRED
  action,             // 'ADD'|'SET'|'DELETE', REQUIRED
  entries,            // FaFEntry[], REQUIRED (min 1 for ADD/DELETE, can be [] for SET to clear all)
})
```

### Validation
- `action` must be one of `['ADD', 'SET', 'DELETE']`
- `entries` must be array (can be empty for SET, but not for ADD/DELETE)
- Each entry must have `fafNumber`

### XML params
```js
{
  ...commonParams(subscriberNumber),
  fafAction: action,
  fafInformation: entries.map(e => ({
    fafNumber: e.fafNumber,
    owner:     e.owner ?? 'Subscriber',
  }))
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 104  | Temporary blocked |
| 126  | Account not active |
| 130  | FaF number not allowed (international restriction, etc.) |

### Response data shape
```js
{ action, count: entries.length }
```

---

## U10 — getRefillOptions.js *(Licensed)*

**Purpose:** Returns available refill profiles for a subscriber. Use this to populate
a recharge screen before calling Refill.

### Function signature
```js
export default async function getRefillOptions({
  subscriberNumber,      // string, REQUIRED
  voucherCode,           // string, optional — filter by specific voucher
  serviceClassCurrent,   // number, optional — filter by service class
})
```

### XML params
```js
{
  ...commonParams(subscriberNumber),
  voucherActivationCode: voucherCode         ?? undefined,
  serviceClassCurrent:   serviceClassCurrent ?? undefined,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 115  | Voucher group service class error |
| 126  | Account not active |

### Response data shape
```js
{
  options: [
    {
      profileId:               number,   // refillProfileID — pass to Refill
      amount:                  string,   // refillAmount in smallest unit
      currency:                string,   // refillCurrency
      serviceClassNew:         number,   // plan that applies after this refill
      supervisionExtensionDays: number, // supervisionExtension
    }
  ]
}
```

### Parsing rules
- `refillInformation` absent → `{ options: [] }`
- Single option → normalise to array

---

## U11 — refill.js

**Purpose:** Perform a top-up. Two modes — voucherless (uses a profile + amount) or
voucher (validates and applies a physical/digital code). Pass one set of params, not both.

### Function signature
```js
export default async function refill({
  subscriberNumber,    // string, REQUIRED

  // VOUCHERLESS mode (XOR with voucher mode)
  amount,              // string, conditional — transactionAmount
  currency,            // string, conditional — transactionCurrency
  profileId,           // string, conditional — refillProfileID

  // VOUCHER mode (XOR with voucherless mode)
  voucherCode,         // string, conditional — voucherActivationCode

  // Optional for both modes
  transactionType,     // string, optional — e.g. 'TOPUP'
  transactionCode,     // string, optional — e.g. 'WEB', 'USSD', 'API'
})
```

### Validation
- EITHER (`amount` AND `currency` AND `profileId`) OR `voucherCode` must be present
- Both sets cannot be provided at the same time — throw if both are present
- Throw with clear message: "refill: provide either (amount+currency+profileId) or voucherCode, not both"

### XML params — voucherless
```js
{
  ...commonParams(subscriberNumber),
  transactionType,
  transactionCode,
  transactionAmount:   amount,
  transactionCurrency: currency,
  refillProfileID:     profileId,
}
```

### XML params — voucher
```js
{
  ...commonParams(subscriberNumber),
  transactionType,
  transactionCode,
  voucherActivationCode: voucherCode,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 103  | Account barred for refill |
| 104  | Temporary blocked |
| 105  | Dedicated account not allowed |
| 107  | Voucher already used by this subscriber |
| 108  | Voucher already used by different subscriber |
| 109  | Voucher status unavailable |
| 110  | Voucher expired |
| 111  | Voucher stolen or missing |
| 112  | Voucher damaged |
| 113  | Voucher status pending |
| 114  | Voucher type not accepted |
| 115  | Voucher group service class mismatch |
| 117  | Service class change not allowed |
| 119  | Invalid voucher activation code |
| 120  | Invalid refill profile |
| 121  | Supervision period too long |
| 122  | Service fee period too long |
| 123  | Max credit limit exceeded |
| 126  | Account not active |

### Response data shape
```js
{
  accountValueAfter:     string,    // new main balance after refill
  supervisionExpiryDate: Date|null, // new supervision expiry
  serviceFeeExpiryDate:  Date|null, // new service fee date
  transactionId:         string,    // AIR-assigned transaction ID
  // Voucher-specific (null for voucherless refill):
  voucherAmount:         string|null,
  voucherCurrency:       string|null,
  voucherSerialNumber:   string|null,
}
```

---

## U12 — updateCommunityList.js

**Purpose:** Set (replace entirely) the subscriber's community list. Pass an empty
array to remove all communities.

### Function signature
```js
export default async function updateCommunityList({
  subscriberNumber,   // string, REQUIRED
  communityIds,       // number[], REQUIRED (pass [] to clear all)
})
```

### Validation
- `communityIds` must be an array (can be empty)

### XML params
```js
{
  ...commonParams(subscriberNumber),
  communityInformationNew: communityIds.map(id => ({ communityID: id })),
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 104  | Temporary blocked |
| 147  | Invalid old community list |
| 148  | Invalid new community list — one or more IDs are invalid |

### Response data shape
```js
{ communityIds }   // echo back the new list
```

---

## U13 — updateSubscriberSegmentation.js

**Purpose:** Move a subscriber to a different account group (segment). Used for
analytics targeting, pricing tiers, and campaign segmentation.

### Function signature
```js
export default async function updateSubscriberSegmentation({
  subscriberNumber,   // string, REQUIRED
  accountGroupId,     // number, REQUIRED
})
```

### Validation
- `accountGroupId` must be a positive integer

### XML params
```js
{
  ...commonParams(subscriberNumber),
  accountGroupID: accountGroupId,
}
```

### Response codes
| Code | Meaning |
|------|---------|
| 0    | Success |
| 100  | Other error |
| 102  | Subscriber not found |
| 104  | Temporary blocked |

### Response data shape
```js
{ accountGroupId }   // echo back the new group
```
