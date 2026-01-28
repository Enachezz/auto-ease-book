package com.api.auto_ease.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import static java.time.LocalDateTime.now;

@MappedSuperclass
@Getter
@Setter
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "created_date")
    LocalDateTime createdDate;

    @Column(name = "modified_date")
    LocalDateTime modifiedDate;

    @PreUpdate
    void onUpdate() {
        modifiedDate = now();
    }

    @PrePersist
    void onPersist() {
        modifiedDate = createdDate = now();
    }
}
