package com.api.auto_ease.domain.garageMake;

import com.api.auto_ease.domain.carMake.CarMake;
import com.api.auto_ease.domain.garage.Garage;
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
        name = "garage_make",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_garage_make_garage_make",
                columnNames = {"garage_id", "make_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class GarageMakeAssignment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "garage_id", nullable = false)
    private Garage garage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "make_id", nullable = false)
    private CarMake make;

    public GarageMakeAssignment(Garage garage, CarMake make) {
        this.garage = garage;
        this.make = make;
    }
}
