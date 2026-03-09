# Types — JSDoc Type Definitions

All types are defined as JSDoc `@typedef` in `src/types/index.js`.
Import them with `/** @type {import('../types').TypeName} */`.

---

## Base Types

### AirResponse (base for ALL method returns)
```js
/**
 * @typedef {object} AirResponse
 * @property {boolean} success         - true only when responseCode === 0
 * @property {number}  responseCode    - AIR response code (0=ok, -1=network, -2=parse, -3=validation)
 * @property {string}  responseMessage - Human-readable status
 * @property {string|null} transactionId - The originTransactionID sent/echoed
 * @property {object|null} data        - Parsed payload (null on any error)
 * @property {object|null} raw         - Full parsed XML response object
 */
```

---

## ACIP Types

### InstallSubscriberParams
```js
/**
 * @typedef {object} InstallSubscriberParams
 * @property {string}  subscriberNumber           - MSISDN
 * @property {number}  serviceClassNew            - Initial service class ID
 * @property {boolean} [temporaryBlockedFlag]     - Block on creation? Default: false
 * @property {number}  [languageIDNew]            - Language ID (default: 1)
 * @property {number}  [ussdEndOfCallNotificationID] - EOCN ID 0-199, 255=none (default: 255)
 */
```

### DeleteSubscriberParams
```js
/**
 * @typedef {object} DeleteSubscriberParams
 * @property {string} subscriberNumber
 * @property {string} [originOperatorID]
 */
```

### LinkSubordinateParams
```js
/**
 * @typedef {object} LinkSubordinateParams
 * @property {string} subscriberNumber    - The subordinate MSISDN
 * @property {string} masterAccountNumber - The master MSISDN
 * @property {string} [originOperatorID]
 */
```

### UpdateTemporaryBlockedParams
```js
/**
 * @typedef {object} UpdateTemporaryBlockedParams
 * @property {string}  subscriberNumber
 * @property {boolean} blocked - true = block, false = unblock
 */
```

### UpdatePromotionCountersParams
```js
/**
 * @typedef {object} UpdatePromotionCountersParams
 * @property {string} subscriberNumber
 * @property {string} [transactionCurrency]           - Required if updating amount
 * @property {string} [promotionRefillAmountRelative] - Amount to add to counter
 */
```

### UpdatePromotionPlanParams
```js
/**
 * @typedef {object} UpdatePromotionPlanParams
 * @property {string} subscriberNumber
 * @property {'ADD'|'SET'|'DELETE'} action
 * @property {string} [planId]            - Mandatory for ADD, optional for SET
 * @property {Date|string} [oldStartDate] - Mandatory for SET/DELETE
 * @property {Date|string} [oldEndDate]   - Mandatory for SET/DELETE
 * @property {Date|string} [startDate]    - Mandatory for ADD/SET
 * @property {Date|string} [endDate]      - Mandatory for ADD/SET
 */
```

### AccumulatorEntry
```js
/**
 * @typedef {object} AccumulatorEntry
 * @property {number}       accumulatorId
 * @property {number}       [relativeValue]   - XOR with absoluteValue
 * @property {number}       [absoluteValue]   - XOR with relativeValue
 * @property {Date|string}  [startDate]
 */
```

### UpdateAccumulatorsParams
```js
/**
 * @typedef {object} UpdateAccumulatorsParams
 * @property {string}             subscriberNumber
 * @property {AccumulatorEntry[]} accumulators     - 1 or more entries
 * @property {number}             [serviceClassCurrent]
 * @property {string}             [originOperatorID]
 */
```

### UpdateRefillBarringParams
```js
/**
 * @typedef {object} UpdateRefillBarringParams
 * @property {string}            subscriberNumber
 * @property {'BAR'|'CLEAR'|'STEP'} action
 */
```

---

## UCIP Types

### UpdateBalanceAndDateParams
```js
/**
 * @typedef {object} DedicatedAccountUpdate
 * @property {number} accountId
 * @property {string} [relativeAmount]   - XOR with absoluteValue
 * @property {string} [absoluteValue]    - XOR with relativeAmount (dedicatedAccountValueNew)
 */

/**
 * @typedef {object} UpdateBalanceAndDateParams
 * @property {string}                   subscriberNumber
 * @property {string}                   [currency]              - Required if changing any balance
 * @property {string}                   [adjustmentAmount]      - Main balance relative adjustment
 * @property {Date|string}              [supervisionExpiryDate]
 * @property {Date|string}              [serviceFeeExpiryDate]
 * @property {number}                   [creditClearancePeriod] - days
 * @property {number}                   [serviceRemovalPeriod]  - days
 * @property {DedicatedAccountUpdate[]} [dedicatedAccounts]
 */
```

### GetAccountDetailsParams
```js
/**
 * @typedef {object} GetAccountDetailsParams
 * @property {string}  subscriberNumber
 * @property {boolean} [includeLocation] - Set true to get locationInformation
 */
```

### UpdateServiceClassParams
```js
/**
 * @typedef {object} UpdateServiceClassParams
 * @property {string}                               subscriberNumber
 * @property {'SetOriginal'|'SetTemporary'|'DeleteTemporary'} action
 * @property {number}                               [serviceClassNew]
 */
```

### FaFEntry
```js
/**
 * @typedef {object} FaFEntry
 * @property {string} fafNumber
 * @property {string} [owner]  - 'Subscriber' or 'Operator' (default: 'Subscriber')
 */
```

### UpdateFaFListParams
```js
/**
 * @typedef {object} UpdateFaFListParams
 * @property {string}          subscriberNumber
 * @property {'ADD'|'SET'|'DELETE'} action
 * @property {FaFEntry[]}      entries
 */
```

### VoucherlessRefillParams
```js
/**
 * @typedef {object} VoucherlessRefillParams
 * @property {string} subscriberNumber
 * @property {string} amount            - transactionAmount
 * @property {string} currency          - transactionCurrency
 * @property {string} profileId         - refillProfileID
 * @property {string} [transactionType]
 * @property {string} [transactionCode]
 */
```

### VoucherRefillParams
```js
/**
 * @typedef {object} VoucherRefillParams
 * @property {string} subscriberNumber
 * @property {string} voucherCode       - voucherActivationCode
 * @property {string} [transactionType]
 * @property {string} [transactionCode]
 */
```

### UpdateCommunityListParams
```js
/**
 * @typedef {object} UpdateCommunityListParams
 * @property {string}   subscriberNumber
 * @property {number[]} communityIds    - Full replacement list. Pass [] to remove all.
 */
```

### UpdateSubscriberSegmentationParams
```js
/**
 * @typedef {object} UpdateSubscriberSegmentationParams
 * @property {string} subscriberNumber
 * @property {number} accountGroupId
 */
```

---

## Response Data Types

### BalanceData (GetBalanceAndDate response)
```js
/**
 * @typedef {object} DedicatedAccount
 * @property {number}      id
 * @property {string}      value
 * @property {Date|null}   expiryDate
 */

/**
 * @typedef {object} BalanceData
 * @property {string}            accountValue
 * @property {Date|null}         supervisionExpiryDate
 * @property {Date|null}         serviceFeeExpiryDate
 * @property {number}            creditClearancePeriod
 * @property {number}            serviceRemovalPeriod
 * @property {boolean}           temporaryBlocked
 * @property {number}            serviceClassCurrent
 * @property {number}            languageId
 * @property {number}            eocnId
 * @property {DedicatedAccount[]} dedicatedAccounts
 */
```

### AccumulatorData (GetAccumulators response)
```js
/**
 * @typedef {object} AccumulatorData
 * @property {number}    id
 * @property {string}    value
 * @property {Date|null} startDate
 * @property {Date|null} expiryDate
 * @property {string}    threshold
 */
```

### AccountDetailsData (GetAccountDetails response)
```js
/**
 * @typedef {object} LocationInfo
 * @property {string}    currentVLR
 * @property {string}    currentMSC
 * @property {boolean}   roaming
 * @property {Date|null} timestamp
 */

/**
 * @typedef {object} AccountDetailsData
 * @property {number}          serviceClassCurrent
 * @property {boolean}         temporaryBlocked
 * @property {Date|null}       supervisionExpiryDate
 * @property {Date|null}       serviceFeeExpiryDate
 * @property {number}          languageId
 * @property {number}          eocnId
 * @property {string}          accountHomeRegion
 * @property {LocationInfo|null} location
 * @property {number[]}        communityIds
 */
```

### RefillData (Refill response)
```js
/**
 * @typedef {object} RefillData
 * @property {string}    accountValueAfter
 * @property {Date|null} supervisionExpiryDate
 * @property {Date|null} serviceFeeExpiryDate
 * @property {string}    transactionId
 * @property {string|null} voucherAmount
 * @property {string|null} voucherCurrency
 * @property {string|null} voucherSerialNumber
 */
```

### PromotionCounterData
```js
/**
 * @typedef {object} PromotionCounter
 * @property {number}    id
 * @property {string}    value
 * @property {Date|null} expiryDate
 * @property {number}    planProgressCounter
 */

/**
 * @typedef {object} PromotionCountersData
 * @property {PromotionCounter[]} counters
 * @property {string}             refillAmountAccumulated
 */
```

### PromotionPlanData
```js
/**
 * @typedef {object} PromotionPlan
 * @property {number}    id
 * @property {Date|null} startDate
 * @property {Date|null} endDate
 */
```

### RefillOptionData
```js
/**
 * @typedef {object} RefillOption
 * @property {number}  profileId
 * @property {string}  amount
 * @property {string}  currency
 * @property {number}  serviceClassNew
 * @property {number}  supervisionExtensionDays
 */
```
