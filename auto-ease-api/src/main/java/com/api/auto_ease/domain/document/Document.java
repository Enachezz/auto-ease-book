package com.api.auto_ease.domain.document;

import com.api.auto_ease.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "DOCUMENT")
public class Document extends BaseEntity {

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "name")
    private String name;

    @Column(name = "s3_path", nullable = false)
    private String s3Path;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "note")
    private String note;
}
