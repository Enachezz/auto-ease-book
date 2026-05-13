package com.api.auto_ease.repository.quoteLog;

import com.api.auto_ease.domain.quoteLog.QuoteLog;
import com.api.auto_ease.domain.quoteLog.QuoteLogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteLogRepository extends JpaRepository<QuoteLog, UUID> {

    Page<QuoteLog> findByTypeOfNotificationAndReferenceAndNotificationFlag(QuoteLogType typeOfNotification,
                                                                          String reference,
                                                                          Boolean notificationFlag,
                                                                          Pageable pageable);

    Page<QuoteLog> findByTypeOfNotificationAndReference(QuoteLogType typeOfNotification,
                                                       String reference,
                                                       Pageable pageable);

    Optional<QuoteLog> findFirstByTriggeredQuoteIdAndTypeOfNotificationAndNotificationFlag(UUID triggeredQuoteId,
                                                                                          QuoteLogType typeOfNotification,
                                                                                          Boolean notificationFlag);

    List<QuoteLog> findByQuoteIdOrderByCreatedDateAsc(UUID quoteId);
}
