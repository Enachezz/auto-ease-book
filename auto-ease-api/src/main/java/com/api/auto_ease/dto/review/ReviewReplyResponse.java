package com.api.auto_ease.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyResponse {

    private UUID id;
    private String authorUserId;
    private String authorRole;
    private String message;
    private UUID parentReplyId;
    private LocalDateTime createdDate;
}
