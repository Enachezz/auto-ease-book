package com.api.auto_ease.service.garage;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.garageCategory.GarageCategoryAssignment;
import com.api.auto_ease.domain.garageMake.GarageMakeAssignment;
import com.api.auto_ease.dto.garage.GarageFilterCriteria;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

final class GarageSpecifications {

    private GarageSpecifications() {
    }

    static Specification<Garage> fromFilter(GarageFilterCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(criteria.getBusinessName())) {
                String pattern = "%" + criteria.getBusinessName().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("businessName")), pattern));
            }
            if (criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty()) {
                List<UUID> categoryIds = criteria.getCategoryIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                if (!categoryIds.isEmpty()) {
                    Subquery<Integer> hasCategory = query.subquery(Integer.class);
                    Root<GarageCategoryAssignment> assign = hasCategory.from(GarageCategoryAssignment.class);
                    hasCategory.select(cb.literal(1));
                    hasCategory.where(
                            cb.and(
                                    cb.equal(assign.get("garage").get("id"), root.get("id")),
                                    assign.get("category").get("id").in(categoryIds)
                            )
                    );
                    predicates.add(cb.exists(hasCategory));
                }
            }
            if (criteria.getIsDealership() != null) {
                predicates.add(cb.equal(root.get("dealership"), criteria.getIsDealership()));
            }
            if (criteria.getMakeId() != null) {
                Subquery<Integer> hasMake = query.subquery(Integer.class);
                Root<GarageMakeAssignment> makeAssign = hasMake.from(GarageMakeAssignment.class);
                hasMake.select(cb.literal(1));
                hasMake.where(
                        cb.and(
                                cb.equal(makeAssign.get("garage").get("id"), root.get("id")),
                                cb.equal(makeAssign.get("make").get("id"), criteria.getMakeId())
                        )
                );
                predicates.add(cb.exists(hasMake));
            }
            if (criteria.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), criteria.getMinRating()));
            }
            if (criteria.getMinReviews() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalReviews"), criteria.getMinReviews()));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
