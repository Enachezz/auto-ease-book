package com.api.auto_ease.repository.specialization;

import com.api.auto_ease.domain.specialization.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, String> {
}


