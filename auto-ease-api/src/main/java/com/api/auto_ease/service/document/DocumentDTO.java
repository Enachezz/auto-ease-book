package com.api.auto_ease.service.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DocumentDTO {

    private String vin;
    private List<DocumentServiceEntryDTO> entries;
}
