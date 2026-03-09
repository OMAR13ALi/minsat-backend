package com.minsat.controller;

import com.minsat.air.acip.*;
import com.minsat.air.model.AirResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/acip")
public class AcipController {

    private final InstallSubscriber installSubscriber;
    private final DeleteSubscriber deleteSubscriber;
    private final LinkSubordinateSubscriber linkSubordinateSubscriber;
    private final UpdateTemporaryBlocked updateTemporaryBlocked;
    private final GetPromotionCounters getPromotionCounters;
    private final UpdatePromotionCounters updatePromotionCounters;
    private final GetPromotionPlans getPromotionPlans;
    private final UpdatePromotionPlan updatePromotionPlan;
    private final UpdateAccumulators updateAccumulators;
    private final UpdateRefillBarring updateRefillBarring;

    public AcipController(
        InstallSubscriber installSubscriber,
        DeleteSubscriber deleteSubscriber,
        LinkSubordinateSubscriber linkSubordinateSubscriber,
        UpdateTemporaryBlocked updateTemporaryBlocked,
        GetPromotionCounters getPromotionCounters,
        UpdatePromotionCounters updatePromotionCounters,
        GetPromotionPlans getPromotionPlans,
        UpdatePromotionPlan updatePromotionPlan,
        UpdateAccumulators updateAccumulators,
        UpdateRefillBarring updateRefillBarring
    ) {
        this.installSubscriber = installSubscriber;
        this.deleteSubscriber = deleteSubscriber;
        this.linkSubordinateSubscriber = linkSubordinateSubscriber;
        this.updateTemporaryBlocked = updateTemporaryBlocked;
        this.getPromotionCounters = getPromotionCounters;
        this.updatePromotionCounters = updatePromotionCounters;
        this.getPromotionPlans = getPromotionPlans;
        this.updatePromotionPlan = updatePromotionPlan;
        this.updateAccumulators = updateAccumulators;
        this.updateRefillBarring = updateRefillBarring;
    }

    @PostMapping("/install")
    public ResponseEntity<AirResponse> install(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(installSubscriber.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<AirResponse> delete(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(deleteSubscriber.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/link-subordinate")
    public ResponseEntity<AirResponse> linkSubordinate(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(linkSubordinateSubscriber.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-blocked")
    public ResponseEntity<AirResponse> updateBlocked(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateTemporaryBlocked.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/promotion-counters")
    public ResponseEntity<AirResponse> promotionCounters(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getPromotionCounters.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-promotion-counters")
    public ResponseEntity<AirResponse> updatePromotionCounters(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updatePromotionCounters.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/promotion-plans")
    public ResponseEntity<AirResponse> promotionPlans(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(getPromotionPlans.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-promotion-plan")
    public ResponseEntity<AirResponse> updatePromotionPlan(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updatePromotionPlan.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-accumulators")
    public ResponseEntity<AirResponse> updateAccumulators(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateAccumulators.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }

    @PostMapping("/update-refill-barring")
    public ResponseEntity<AirResponse> updateRefillBarring(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(updateRefillBarring.execute(body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AirResponse.validationError(e.getMessage()));
        }
    }
}
