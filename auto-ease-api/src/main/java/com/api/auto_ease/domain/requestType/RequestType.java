package com.api.auto_ease.domain.requestType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "REQUEST_TYPE")
public class RequestType {

    @Id
    @Column(name = "type")
    private String type;

    @Column(name = "description")
    private String description;
}
