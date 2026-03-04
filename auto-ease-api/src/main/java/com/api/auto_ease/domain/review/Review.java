package com.api.auto_ease.domain.review;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "garage_id", nullable = false)
    private UUID garageId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @PrePersist
    void onPersist() {
        if (id == null) id = UUID.randomUUID();
        modifiedDate = createdDate = now();
    }

    @PreUpdate
    void onUpdate() {
        modifiedDate = now();
    }
}
