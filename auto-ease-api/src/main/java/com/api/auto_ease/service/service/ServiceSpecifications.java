package com.api.auto_ease.service.service;

import com.api.auto_ease.domain.service.ServiceEntity;
import com.api.auto_ease.dto.service.ServiceFilterCriteria;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

final class ServiceSpecifications {

    private ServiceSpecifications() {
    }

    static Specification<ServiceEntity> fromFilter(ServiceFilterCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(criteria.getName())) {
                String pattern = "%" + criteria.getName().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
