package com.api.auto_ease.domain.quoteLog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "quote_logs")
public class QuoteLog {

    @Id
    @GeneratedValue
    @Column(updatable = false)
    private UUID id;

    @Column(name = "quote_id", nullable = false)
    private UUID quoteId;

    @Column(name = "author_user_id", nullable = false, length = 50)
    private String authorUserId;

    @Column(name = "message")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_of_notification", nullable = false, length = 20)
    private QuoteLogType typeOfNotification;

    @Column(name = "reference", nullable = false, length = 50)
    private String reference;

    @Column(name = "notification_flag", nullable = false)
    private Boolean notificationFlag;

    @Column(name = "triggered_quote_id")
    private UUID triggeredQuoteId;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @PrePersist
    void onPersist() {
        if (notificationFlag == null) {
            notificationFlag = Boolean.TRUE;
        }
        modifiedDate = createdDate = now();
    }

    @PreUpdate
    void onUpdate() {
        modifiedDate = now();
    }
}
