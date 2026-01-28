package com.api.auto_ease.domain.rating;

import com.api.auto_ease.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "RATING")
public class Rating extends BaseEntity {

    @Column(name = "rating")
    private Integer rating;

    @Column(name= "service_entry_id")
    private int serviceEntryId;
}
