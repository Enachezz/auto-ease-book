package com.api.auto_ease.service.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DocumentServiceEntryDTO {

    private LocalDate date;
    private String type;
    private String description;
}
