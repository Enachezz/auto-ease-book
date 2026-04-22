package com.api.auto_ease.adapter;

import com.api.auto_ease.service.document.DocumentDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DocumentAdapter {

    private final RestTemplate restTemplate = new RestTemplate();

    public String doCallToApi(DocumentDTO documentDTO) {

        return "url-to-download-pdf";
    }
}
