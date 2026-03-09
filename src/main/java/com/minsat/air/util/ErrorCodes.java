package com.minsat.air.util;

import java.util.Map;

public class ErrorCodes {

    public record ErrorInfo(String message, String category) {}

    private static final Map<Integer, ErrorInfo> CODES = Map.ofEntries(
        Map.entry(0,   new ErrorInfo("Request succeeded", "success")),
        Map.entry(-1,  new ErrorInfo("Network error", "system")),
        Map.entry(-2,  new ErrorInfo("XML parse error", "system")),
        Map.entry(-3,  new ErrorInfo("Validation error", "system")),
        Map.entry(100, new ErrorInfo("Other error — verify AF configuration", "system")),
        Map.entry(102, new ErrorInfo("Subscriber not found", "subscriber")),
        Map.entry(103, new ErrorInfo("Account barred for refill", "account")),
        Map.entry(104, new ErrorInfo("Temporary blocked", "account")),
        Map.entry(105, new ErrorInfo("Dedicated account not allowed", "account")),
        Map.entry(106, new ErrorInfo("Dedicated account negative balance", "account")),
        Map.entry(107, new ErrorInfo("Voucher already used by same subscriber", "voucher")),
        Map.entry(108, new ErrorInfo("Voucher already used by different subscriber", "voucher")),
        Map.entry(109, new ErrorInfo("Voucher status unavailable", "voucher")),
        Map.entry(110, new ErrorInfo("Voucher expired", "voucher")),
        Map.entry(111, new ErrorInfo("Voucher stolen or missing", "voucher")),
        Map.entry(112, new ErrorInfo("Voucher damaged", "voucher")),
        Map.entry(113, new ErrorInfo("Voucher status pending", "voucher")),
        Map.entry(114, new ErrorInfo("Voucher type not accepted", "voucher")),
        Map.entry(115, new ErrorInfo("Voucher group service class error", "voucher")),
        Map.entry(117, new ErrorInfo("Service class change not allowed", "account")),
        Map.entry(119, new ErrorInfo("Invalid voucher activation code", "voucher")),
        Map.entry(120, new ErrorInfo("Invalid refill profile", "account")),
        Map.entry(121, new ErrorInfo("Supervision period too long", "account")),
        Map.entry(122, new ErrorInfo("Service fee period too long", "account")),
        Map.entry(123, new ErrorInfo("Max credit limit exceeded", "account")),
        Map.entry(124, new ErrorInfo("Below minimum balance", "account")),
        Map.entry(126, new ErrorInfo("Account not active", "account")),
        Map.entry(127, new ErrorInfo("Accumulator not available", "account")),
        Map.entry(128, new ErrorInfo("Invalid PIN code", "account")),
        Map.entry(130, new ErrorInfo("FaF number not allowed", "account")),
        Map.entry(133, new ErrorInfo("Service class list empty", "account")),
        Map.entry(134, new ErrorInfo("Accumulator overflow", "account")),
        Map.entry(135, new ErrorInfo("Accumulator underflow", "account")),
        Map.entry(136, new ErrorInfo("Date adjustment error", "account")),
        Map.entry(137, new ErrorInfo("GetBalanceAndDate not allowed", "account")),
        Map.entry(138, new ErrorInfo("No PIN code registered", "account")),
        Map.entry(139, new ErrorInfo("Dedicated account not defined", "account")),
        Map.entry(140, new ErrorInfo("Invalid old service class", "account")),
        Map.entry(141, new ErrorInfo("Invalid language", "account")),
        Map.entry(142, new ErrorInfo("Subscriber already installed", "subscriber")),
        Map.entry(143, new ErrorInfo("Invalid master subscriber", "subscriber")),
        Map.entry(144, new ErrorInfo("Subscriber already activated", "subscriber")),
        Map.entry(145, new ErrorInfo("Already linked as subordinate", "subscriber")),
        Map.entry(146, new ErrorInfo("Already linked as master", "subscriber")),
        Map.entry(147, new ErrorInfo("Invalid old community list", "account")),
        Map.entry(148, new ErrorInfo("Invalid new community list", "account")),
        Map.entry(149, new ErrorInfo("Invalid promotion plan end date", "plan")),
        Map.entry(150, new ErrorInfo("Invalid promotion plan ID", "plan")),
        Map.entry(151, new ErrorInfo("Promotion plan not found", "plan")),
        Map.entry(152, new ErrorInfo("Deblocking of expired account", "account")),
        Map.entry(153, new ErrorInfo("Dedicated account max credit limit exceeded", "account")),
        Map.entry(154, new ErrorInfo("Invalid old service class date", "account")),
        Map.entry(155, new ErrorInfo("Invalid new service class", "account")),
        Map.entry(156, new ErrorInfo("Delete failed", "subscriber")),
        Map.entry(157, new ErrorInfo("Invalid account home region", "account")),
        Map.entry(158, new ErrorInfo("Max promotion plans already reached", "plan"))
    );

    private ErrorCodes() {}

    public static ErrorInfo describe(int code) {
        return CODES.getOrDefault(code, new ErrorInfo("Unknown response code: " + code, "unknown"));
    }

    public static String message(int code) {
        return describe(code).message();
    }

    public static boolean isRetryable(int code) {
        return code == -1 || code == 100;
    }
}
