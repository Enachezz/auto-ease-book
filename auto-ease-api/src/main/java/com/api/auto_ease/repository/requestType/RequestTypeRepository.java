package com.api.auto_ease.repository.requestType;

import com.api.auto_ease.domain.requestType.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestTypeRepository extends JpaRepository<RequestType, Long> {
}
