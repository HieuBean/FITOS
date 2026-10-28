package com.fitos.service;

import com.fitos.client.LeadAIClient;
import com.fitos.client.dto.LeadScoringRequest;
import com.fitos.client.dto.LeadScoringResponse;
import com.fitos.dto.lead.*;
import com.fitos.exception.BadRequestException;
import com.fitos.exception.ResourceNotFoundException;
import com.fitos.model.Lead;
import com.fitos.model.LeadActivity;
import com.fitos.model.Customer;
import com.fitos.model.User;
import com.fitos.repository.LeadActivityRepository;
import com.fitos.repository.LeadRepository;
import com.fitos.repository.UserRepository;
import com.fitos.security.StudioContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final UserRepository userRepository;
    private final com.fitos.repository.CustomerRepository customerRepository;
    private final LeadAIClient leadAIClient;
    private final StudioAccessService studioAccessService;

    @Transactional
    public LeadResponse createLead(LeadCreateRequest req, Long creatorUserId) {
        Long studioId = StudioContext.currentStudioId();
        User assignedUser = null;
        if (req.getAssignedToUserId() != null) {
            assignedUser = userRepository.findById(req.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found: " + req.getAssignedToUserId()));
            if (!studioAccessService.studioIds(assignedUser.getId()).contains(studioId)) {
                throw new BadRequestException("Assigned user does not belong to this studio");
            }
        } else if (creatorUserId != null) {
            assignedUser = userRepository.findById(creatorUserId).orElse(null);
        }

        Lead lead = Lead.builder()
                .studioId(studioId)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .email(req.getEmail())
                .dob(req.getDob())
                .goal(normalizeGoal(req.getGoal()))
                .source(normalizeSource(req.getSource()))
                .status("NEW")
                .assignedTo(assignedUser)
                .notes(req.getNotes())
                .build();

        lead = leadRepository.save(lead);

        // Add initial activity
        LeadActivity initialActivity = LeadActivity.builder()
                .studioId(studioId)
                .lead(lead)
                .user(assignedUser)
                .activityType("NOTE")
                .notes("Lead created from source: " + lead.getSource())
                .activityTime(Instant.now())
                .build();
        leadActivityRepository.save(initialActivity);

        return mapToResponse(lead, true);
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> getLeads(String status, String source, String search, Pageable pageable) {
        Page<Lead> leads = leadRepository.findWithFilters(StudioContext.currentStudioId(), status, source, search, pageable);
        return leads.map(l -> mapToResponse(l, false));
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(Long id) {
        Lead lead = leadRepository.findByIdAndStudioId(id, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
        return mapToResponse(lead, true);
    }

    @Transactional
    public LeadResponse updateLead(Long id, LeadUpdateRequest req) {
        Lead lead = leadRepository.findByIdAndStudioId(id, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        lead.setFullName(req.getFullName());
        lead.setPhone(req.getPhone());
        lead.setEmail(req.getEmail());
        lead.setDob(req.getDob());
        if (req.getGoal() != null) lead.setGoal(req.getGoal());
        if (req.getSource() != null) lead.setSource(req.getSource());
        if (req.getStatus() != null && !req.getStatus().equals(lead.getStatus())) {
            throw new BadRequestException("Use the status endpoint to change lead status");
        }
        if (req.getNotes() != null) lead.setNotes(req.getNotes());
        if (req.getLostReason() != null) lead.setLostReason(req.getLostReason());

        if (req.getAssignedToUserId() != null) {
            User assignedUser = userRepository.findById(req.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found: " + req.getAssignedToUserId()));
            if (!studioAccessService.studioIds(assignedUser.getId()).contains(lead.getStudioId())) {
                throw new BadRequestException("Assigned user does not belong to this studio");
            }
            lead.setAssignedTo(assignedUser);
        }

        lead = leadRepository.save(lead);
        return mapToResponse(lead, true);
    }

    @Transactional
    public LeadResponse updateStatus(Long id, LeadStatusUpdateRequest req, Long actorUserId) {
        Lead lead = leadRepository.findByIdAndStudioId(id, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        String oldStatus = lead.getStatus();
        if ("CONVERTED".equals(oldStatus) || "CONVERTED".equals(req.getStatus())) {
            throw new BadRequestException("Use lead conversion to set CONVERTED status");
        }
        if ("LOST".equals(oldStatus)) {
            throw new BadRequestException("A lost lead must be reopened by a manager");
        }
        if ("LOST".equals(req.getStatus()) && (req.getLostReason() == null || req.getLostReason().isBlank())) {
            throw new BadRequestException("Lost reason is required");
        }
        lead.setStatus(req.getStatus());
        if (req.getLostReason() != null) {
            lead.setLostReason(req.getLostReason());
        }

        User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;

        // Record status change activity
        LeadActivity activity = LeadActivity.builder()
                .lead(lead)
                .user(actor)
                .activityType("FOLLOW_UP")
                .notes("Status changed from " + oldStatus + " to " + req.getStatus() + (req.getNotes() != null ? ": " + req.getNotes() : ""))
                .activityTime(Instant.now())
                .build();
        leadActivityRepository.save(activity);

        lead = leadRepository.save(lead);
        return mapToResponse(lead, true);
    }

    @Transactional
    public LeadActivityResponse addActivity(Long leadId, LeadActivityRequest req, Long actorUserId) {
        Lead lead = leadRepository.findByIdAndStudioId(leadId, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;

        LeadActivity activity = LeadActivity.builder()
                .studioId(lead.getStudioId())
                .lead(lead)
                .user(actor)
                .activityType(req.getActivityType())
                .notes(req.getNotes())
                .activityTime(Instant.now())
                .build();

        activity = leadActivityRepository.save(activity);

        return mapActivityToResponse(activity);
    }

    @Transactional
    public LeadResponse assignLead(Long leadId, Long assignedToUserId, Long actorUserId) {
        Lead lead = leadRepository.findByIdAndStudioId(leadId, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        User assignedUser = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assignedToUserId));
        if (!studioAccessService.studioIds(assignedUser.getId()).contains(lead.getStudioId())) {
            throw new BadRequestException("Assigned user does not belong to this studio");
        }

        lead.setAssignedTo(assignedUser);

        User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;
        LeadActivity activity = LeadActivity.builder()
                .studioId(lead.getStudioId())
                .lead(lead)
                .user(actor)
                .activityType("FOLLOW_UP")
                .notes("Assigned to staff member: " + assignedUser.getFullName())
                .activityTime(Instant.now())
                .build();
        leadActivityRepository.save(activity);

        lead = leadRepository.save(lead);
        return mapToResponse(lead, true);
    }

    @Transactional(readOnly = true)
    public List<LeadActivityResponse> getActivities(Long leadId) {
        if (!leadRepository.existsByIdAndStudioId(leadId, StudioContext.currentStudioId())) {
            throw new ResourceNotFoundException("Lead not found with id: " + leadId);
        }
        return leadActivityRepository.findByLeadIdOrderByActivityTimeDesc(leadId).stream()
                .map(this::mapActivityToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeadResponse convertLead(Long leadId) {
        Lead lead = leadRepository.findByIdAndStudioId(leadId, StudioContext.currentStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));
        if (lead.getConvertedCustomerId() != null || "CONVERTED".equals(lead.getStatus())) {
            throw new BadRequestException("Lead has already been converted");
        }
        Customer customer = customerRepository.findByPhoneAndStudioId(lead.getPhone(), lead.getStudioId())
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .studioId(lead.getStudioId())
                        .leadId(lead.getId())
                        .fullName(lead.getFullName())
                        .phone(lead.getPhone())
                        .email(lead.getEmail())
                        .dob(lead.getDob())
                        .fitnessGoal(lead.getGoal())
                        .status("ACTIVE")
                        .build()));
        lead.setConvertedCustomerId(customer.getId());
        lead.setStatus("CONVERTED");
        leadRepository.save(lead);
        return mapToResponse(lead, true);
    }

    private LeadResponse mapToResponse(Lead lead, boolean includeActivities) {
        List<LeadActivityResponse> activityResponses = null;
        int activitiesCount = 0;

        if (includeActivities) {
            List<LeadActivity> activities = leadActivityRepository.findByLeadIdOrderByActivityTimeDesc(lead.getId());
            activitiesCount = activities.size();
            activityResponses = activities.stream()
                    .map(this::mapActivityToResponse)
                    .collect(Collectors.toList());
        }

        // Call Lead AI Client for scoring
        LeadScoringResponse scoring = leadAIClient.scoreLead(LeadScoringRequest.builder()
                .leadId(lead.getId())
                .fullName(lead.getFullName())
                .goal(lead.getGoal())
                .source(lead.getSource())
                .status(lead.getStatus())
                .activitiesCount(activitiesCount)
                .build());

        return LeadResponse.builder()
                .id(lead.getId())
                .fullName(lead.getFullName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .dob(lead.getDob())
                .goal(lead.getGoal())
                .source(lead.getSource())
                .status(lead.getStatus())
                .assignedToUserId(lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null)
                .assignedToName(lead.getAssignedTo() != null ? lead.getAssignedTo().getFullName() : null)
                .notes(lead.getNotes())
                .lostReason(lead.getLostReason())
                .convertedCustomerId(lead.getConvertedCustomerId())
                .aiScore(scoring != null ? scoring.getScore() : null)
                .aiSuggestedPersona(scoring != null ? scoring.getPersona() : null)
                .activities(activityResponses)
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }

    private LeadActivityResponse mapActivityToResponse(LeadActivity act) {
        return LeadActivityResponse.builder()
                .id(act.getId())
                .leadId(act.getLead().getId())
                .userId(act.getUser() != null ? act.getUser().getId() : null)
                .userName(act.getUser() != null ? act.getUser().getFullName() : null)
                .activityType(act.getActivityType())
                .notes(act.getNotes())
                .activityTime(act.getActivityTime())
                .createdAt(act.getCreatedAt())
                .build();
    }

    private String normalizeSource(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "WALK_IN";
        String s = raw.trim().toUpperCase();
        if (s.contains("INSTA")) return "INSTAGRAM";
        if (s.contains("FACE")) return "FACEBOOK";
        if (s.contains("WEB")) return "WEBSITE";
        if (s.contains("REFER") || s.contains("GIỚI THIỆU") || s.contains("GIOI THIEU")) return "REFERRAL";
        if (s.contains("AD") || s.contains("QUẢNG CÁO") || s.contains("QUANG CAO")) return "ADVERTISEMENT";
        if (s.contains("WALK") || s.contains("TRỰC TIẾP") || s.contains("TRUC TIEP")) return "WALK_IN";
        if (List.of("WALK_IN", "WEBSITE", "FACEBOOK", "INSTAGRAM", "REFERRAL", "ADVERTISEMENT", "OTHER").contains(s)) {
            return s;
        }
        return "OTHER";
    }

    private String normalizeGoal(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "GENERAL_FITNESS";
        String s = raw.trim().toUpperCase();
        if (s.contains("WEIGHT") || s.contains("GIẢM CÂN") || s.contains("GIAM CAN")) return "WEIGHT_LOSS";
        if (s.contains("MUSCLE") || s.contains("TĂNG CƠ") || s.contains("TANG CO")) return "MUSCLE_GAIN";
        if (s.contains("STRENGTH") || s.contains("SỨC MẠNH") || s.contains("SUC MANH")) return "STRENGTH";
        if (s.contains("MOBILITY") || s.contains("DẺO DAI") || s.contains("DEO DAI")) return "MOBILITY";
        if (List.of("WEIGHT_LOSS", "MUSCLE_GAIN", "GENERAL_FITNESS", "STRENGTH", "MOBILITY", "OTHER").contains(s)) {
            return s;
        }
        return "GENERAL_FITNESS";
    }
}
