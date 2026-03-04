package com.api.auto_ease.repository.jobrequest;

import com.api.auto_ease.domain.jobrequest.JobRequest;
import com.api.auto_ease.domain.jobrequest.JobRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRequestRepository extends JpaRepository<JobRequest, UUID> {

    List<JobRequest> findByUserIdOrderByCreatedDateDesc(String userId);

    List<JobRequest> findByStatusOrderByCreatedDateDesc(JobRequestStatus status);
}
