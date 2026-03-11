package com.api.auto_ease.domain.quote;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue
    @Column(updatable = false)
    private UUID id;

    @Column(name = "job_request_id", nullable = false)
    private UUID jobRequestId;

    @Column(name = "garage_id", nullable = false)
    private UUID garageId;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "estimated_duration", length = 100)
    private String estimatedDuration;

    @Column(name = "description")
    private String description;

    @Column(name = "warranty_info")
    private String warrantyInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuoteStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @PrePersist
    void onPersist() {
        if (status == null) status = QuoteStatus.PENDING;
        modifiedDate = createdDate = now();
    }

    @PreUpdate
    void onUpdate() {
        modifiedDate = now();
    }
}
