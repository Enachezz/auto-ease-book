package com.api.auto_ease.domain.garageCategory;

import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.serviceCategory.ServiceCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "garage_category",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_garage_category_garage_category",
                columnNames = {"garage_id", "category_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class GarageCategoryAssignment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    public GarageCategoryAssignment(Garage garage, ServiceCategory category) {
        this.garage = garage;
        this.category = category;
    }
}
