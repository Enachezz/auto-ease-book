package com.api.auto_ease.dto.review;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewReplyRequest {

    @NotBlank
    private String message;

    private UUID parentReplyId;
}
