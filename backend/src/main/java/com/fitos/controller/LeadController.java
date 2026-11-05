package com.fitos.controller;

import com.fitos.dto.lead.*;
import com.fitos.security.UserPrincipal;
import com.fitos.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Lead CRM", description = "Endpoints for managing prospective leads, sales pipeline and activities")
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Create a new lead")
    public ResponseEntity<LeadResponse> createLead(
            @Valid @RequestBody LeadCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return new ResponseEntity<>(leadService.createLead(req, principal != null ? principal.getId() : null), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Get paginated leads with filters")
    public ResponseEntity<Page<LeadResponse>> getLeads(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(leadService.getLeads(status, source, search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Get lead detail with activity timeline")
    public ResponseEntity<LeadResponse> getLeadById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(leadService.getLeadById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Update lead information")
    public ResponseEntity<LeadResponse> updateLead(
            @PathVariable("id") Long id,
            @Valid @RequestBody LeadUpdateRequest req) {
        return ResponseEntity.ok(leadService.updateLead(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Update lead lifecycle status")
    public ResponseEntity<LeadResponse> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody LeadStatusUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.updateStatus(id, req, principal != null ? principal.getId() : null));
    }

    @PostMapping("/{id}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Log an interaction activity on a lead")
    public ResponseEntity<LeadActivityResponse> addActivity(
            @PathVariable("id") Long id,
            @Valid @RequestBody LeadActivityRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return new ResponseEntity<>(leadService.addActivity(id, req, principal != null ? principal.getId() : null), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Get lead activity timeline")
    public ResponseEntity<List<LeadActivityResponse>> getActivities(@PathVariable("id") Long id) {
        return ResponseEntity.ok(leadService.getActivities(id));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Convert a lead into a customer profile")
    public ResponseEntity<LeadResponse> convertLead(@PathVariable("id") Long id) {
        return ResponseEntity.ok(leadService.convertLead(id));
    }
    @PatchMapping("/{id}/assign")    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Assign lead to staff member")
    public ResponseEntity<LeadResponse> assignLead(
            @PathVariable("id") Long id,
            @Valid @RequestBody LeadAssignRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.assignLead(id, req.getAssignedToUserId(), principal != null ? principal.getId() : null));
    }

    @PatchMapping("/{id}/lost")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    @Operation(summary = "Mark a lead as lost with a mandatory reason")
    public ResponseEntity<LeadResponse> markLost(
            @PathVariable("id") Long id,
            @Valid @RequestBody com.fitos.dto.lead.LeadLostRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leadService.markLost(id, req.reason(), principal != null ? principal.getId() : null));
    }
}
