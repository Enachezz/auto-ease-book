package com.api.auto_ease.domain.appUser;

import jakarta.persistence.*;
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
@Table(name = "APP_USER")
public class AppUser {

    @Id
    @Column(name = "uuid", unique = true, nullable = false, length = 50)
    private String uuid;

    @Column(name = "created_date")
    LocalDateTime createdDate;

    @Column(name = "modified_date")
    LocalDateTime modifiedDate;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AppUserType type;

    @PreUpdate
    void onUpdate() {
        modifiedDate = now();
    }

    @PrePersist
    void onPersist() {
        modifiedDate = createdDate = now();
    }
}
