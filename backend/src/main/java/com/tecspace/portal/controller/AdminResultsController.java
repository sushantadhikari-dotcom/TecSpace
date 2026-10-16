package com.tecspace.portal.controller;

import com.tecspace.portal.dto.GradingScaleRequest;
import com.tecspace.portal.dto.TermActionRequest;
import com.tecspace.portal.dto.TermStatus;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.GradingScale;
import com.tecspace.portal.service.GradingScaleService;
import com.tecspace.portal.service.ResultTermService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Phase 3 admin endpoints: grading scale, gradebook locking and report card publishing. */
@RestController
@RequestMapping("/api/admin")
public class AdminResultsController {

    private final GradingScaleService scaleService;
    private final ResultTermService termService;

    public AdminResultsController(GradingScaleService scaleService, ResultTermService termService) {
        this.scaleService = scaleService;
        this.termService = termService;
    }

    @GetMapping("/grading-scale")
    public GradingScale scale() {
        return scaleService.active();
    }

    @PutMapping("/grading-scale")
    public GradingScale updateScale(@Valid @RequestBody GradingScaleRequest req) {
        return scaleService.update(req);
    }

    @GetMapping("/result-terms")
    public List<TermStatus> terms() {
        return termService.list();
    }

    /** action: lock, unlock, publish or unpublish */
    @PostMapping("/result-terms/{action}")
    public ResponseEntity<Void> act(@PathVariable String action, @Valid @RequestBody TermActionRequest req) {
        switch (action) {
            case "lock" -> termService.lock(req.trimesterName());
            case "unlock" -> termService.unlock(req.trimesterName());
            case "publish" -> termService.publish(req.trimesterName());
            case "unpublish" -> termService.unpublish(req.trimesterName());
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown action " + action);
        }
        return ResponseEntity.noContent().build();
    }
}
