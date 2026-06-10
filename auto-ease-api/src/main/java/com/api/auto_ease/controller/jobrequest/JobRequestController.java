package com.api.auto_ease.controller.jobrequest;

import com.api.auto_ease.dto.jobrequest.CreateJobRequestRequest;
import com.api.auto_ease.dto.jobrequest.JobRequestResponse;
import com.api.auto_ease.dto.jobrequest.UpdateJobRequestRequest;
import com.api.auto_ease.service.jobrequest.JobRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_CAR_OWNER;
import static com.api.auto_ease.security.AppUserTypeSecurityExpressions.HAS_ROLE_GARAGE;

@RestController
@RequiredArgsConstructor
public class JobRequestController {

    private final JobRequestService jobRequestService;

    @PostMapping("/api/job-requests")
    @PreAuthorize(HAS_ROLE_CAR_OWNER)
    public ResponseEntity<JobRequestResponse> createJobRequest(Authentication auth,
                                                                @Valid @RequestBody CreateJobRequestRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(jobRequestService.createJobRequest(userId, request));
    }

    @GetMapping("/api/job-requests")
    @PreAuthorize(HAS_ROLE_CAR_OWNER)
    public List<JobRequestResponse> getMyJobRequests(Authentication auth,
                                                      @RequestParam(required = false) UUID categoryId) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.getMyJobRequests(userId, categoryId);
    }

    @GetMapping("/api/job-requests/{id}")
    @PreAuthorize(HAS_ROLE_CAR_OWNER)
    public JobRequestResponse getJobRequest(Authentication auth,
                                             @PathVariable UUID id) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.getJobRequest(userId, id);
    }

    @GetMapping("/api/job-requests/open")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public List<JobRequestResponse> getOpenJobRequests(Authentication auth,
                                                       @RequestParam(required = false) UUID categoryId) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.getOpenJobRequestsForGarageUser(userId, categoryId);
    }

    @PutMapping("/api/job-requests/{id}")
    @PreAuthorize(HAS_ROLE_CAR_OWNER)
    public JobRequestResponse updateJobRequest(Authentication auth,
                                                @PathVariable UUID id,
                                                @RequestBody UpdateJobRequestRequest request) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.updateJobRequest(userId, id, request);
    }

    @DeleteMapping("/api/job-requests/{id}")
    @PreAuthorize(HAS_ROLE_CAR_OWNER)
    public ResponseEntity<Void> deleteJobRequest(Authentication auth,
                                                  @PathVariable UUID id) {
        String userId = (String) auth.getPrincipal();
        jobRequestService.deleteJobRequest(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/job-requests/{id}/complete")
    @PreAuthorize(HAS_ROLE_GARAGE)
    public JobRequestResponse completeJob(Authentication auth, @PathVariable UUID id) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.completeJob(userId, id);
    }
}
