package com.api.auto_ease.domain.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "SERVICE")
public class Service {

    @Id
    @Column(name = "uuid", unique = true, nullable = false, length = 50)
    private String uuid;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "phone")
    private Integer phone;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @PreUpdate
    void onUpdate() {
        modifiedDate = now();
    }

    @PrePersist
    void onPersist() {
        modifiedDate = createdDate = now();
    }
}

