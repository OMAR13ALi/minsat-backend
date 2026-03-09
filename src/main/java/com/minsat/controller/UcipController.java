package com.minsat.controller;

import com.minsat.air.model.AirResponse;
import com.minsat.air.ucip.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ucip")
public class UcipController {

    private final GetBalanceAndDate getBalanceAndDate;
    private final GetAccountDetails getAccountDetails;
    private final GetAccumulators getAccumulators;
    private final GetFaFList getFaFList;
    private final UpdateBalanceAndDate updateBalanceAndDate;
    private final UpdateAccountDetails updateAccountDetails;
    private final GetAllowedServiceClasses getAllowedServiceClasses;
    private final UpdateServiceClass updateServiceClass;
    private final UpdateFaFList updateFaFList;
    private final GetRefillOptions getRefillOptions;
    private final Refill refill;
    private final UpdateCommunityList updateCommunityList;
    private final UpdateSubscriberSegmentation updateSubscriberSegmentation;

    public UcipController(
        GetBalanceAndDate getBalanceAndDate,
        GetAccountDetails getAccountDetails,
        GetAccumulators getAccumulators,
        GetFaFList getFaFList,
        UpdateBalanceAndDate updateBalanceAndDate,
        UpdateAccountDetails updateAccountDetails,
        GetAllowedServiceClasses getAllowedServiceClasses,
        UpdateServiceClass updateServiceClass,
        UpdateFaFList updateFaFList,
        GetRefillOptions getRefillOptions,
        Refill refill,
        UpdateCommunityList updateCommunityList,
        UpdateSubscriberSegmentation updateSubscriberSegmentation
    ) {
        this.getBalanceAndDate = getBalanceAndDate;
        this.getAccountDetails = getAccountDetails;
        this.getAccumulators = getAccumulators;
        this.getFaFList = getFaFList;
        this.updateBalanceAndDate = updateBalanceAndDate;
        this.updateAccountDetails = updateAccountDetails;
        this.getAllowedServiceClasses = getAllowedServiceClasses;
        this.updateServiceClass = updateServiceClass;
        this.updateFaFList = updateFaFList;
        this.getRefillOptions = getRefillOptions;
        this.refill = refill;
        this.updateCommunityList = updateCommunityList;
        this.updateSubscriberSegmentation = updateSubscriberSegmentation;
    }

    @PostMapping("/balance-and-date")
    public ResponseEntity<AirResponse> balanceAndDate(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getBalanceAndDate.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/account-details")
    public ResponseEntity<AirResponse> accountDetails(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getAccountDetails.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/accumulators")
    public ResponseEntity<AirResponse> accumulators(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getAccumulators.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/faf-list")
    public ResponseEntity<AirResponse> fafList(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getFaFList.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-balance")
    public ResponseEntity<AirResponse> updateBalance(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateBalanceAndDate.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-account-details")
    public ResponseEntity<AirResponse> updateAccountDetails(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateAccountDetails.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/allowed-service-classes")
    public ResponseEntity<AirResponse> allowedServiceClasses(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getAllowedServiceClasses.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-service-class")
    public ResponseEntity<AirResponse> updateServiceClass(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateServiceClass.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-faf-list")
    public ResponseEntity<AirResponse> updateFafList(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateFaFList.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/refill-options")
    public ResponseEntity<AirResponse> refillOptions(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getRefillOptions.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/refill")
    public ResponseEntity<AirResponse> refill(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(refill.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-community")
    public ResponseEntity<AirResponse> updateCommunity(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateCommunityList.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-segmentation")
    public ResponseEntity<AirResponse> updateSegmentation(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateSubscriberSegmentation.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }
}
