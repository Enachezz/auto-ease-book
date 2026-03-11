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

@RestController
@RequiredArgsConstructor
public class JobRequestController {

    private final JobRequestService jobRequestService;

    @PostMapping("/api/job-requests")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public ResponseEntity<JobRequestResponse> createJobRequest(Authentication auth,
                                                                @Valid @RequestBody CreateJobRequestRequest request) {
        String userId = (String) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(jobRequestService.createJobRequest(userId, request));
    }

    @GetMapping("/api/job-requests")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public List<JobRequestResponse> getMyJobRequests(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.getMyJobRequests(userId);
    }

    @GetMapping("/api/job-requests/{id}")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public JobRequestResponse getJobRequest(Authentication auth,
                                             @PathVariable UUID id) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.getJobRequest(userId, id);
    }

    @GetMapping("/api/job-requests/open")
    @PreAuthorize("hasRole('GARAGE')")
    public List<JobRequestResponse> getOpenJobRequests() {
        return jobRequestService.getOpenJobRequests();
    }

    @PutMapping("/api/job-requests/{id}")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public JobRequestResponse updateJobRequest(Authentication auth,
                                                @PathVariable UUID id,
                                                @RequestBody UpdateJobRequestRequest request) {
        String userId = (String) auth.getPrincipal();
        return jobRequestService.updateJobRequest(userId, id, request);
    }

    @DeleteMapping("/api/job-requests/{id}")
    @PreAuthorize("hasRole('CAR_OWNER')")
    public ResponseEntity<Void> deleteJobRequest(Authentication auth,
                                                  @PathVariable UUID id) {
        String userId = (String) auth.getPrincipal();
        jobRequestService.deleteJobRequest(userId, id);
        return ResponseEntity.noContent().build();
    }
}
