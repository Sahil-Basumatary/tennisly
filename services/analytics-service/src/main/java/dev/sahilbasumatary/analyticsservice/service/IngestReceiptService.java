package dev.sahilbasumatary.analyticsservice.service;

import dev.sahilbasumatary.analyticsservice.entity.AnalyticsIngestReceipt;
import dev.sahilbasumatary.analyticsservice.repository.AnalyticsIngestReceiptRepository;
import dev.sahilbasumatary.common.event.MatchEvent;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestReceiptService {

    private final AnalyticsIngestReceiptRepository receiptRepository;

    public IngestReceiptService(AnalyticsIngestReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    public boolean alreadyProcessed(String eventId) {
        return receiptRepository.existsByEventId(eventId);
    }

    @Transactional
    public boolean recordIfAbsent(MatchEvent event) {
        if (receiptRepository.existsByEventId(event.getEventId())) {
            return false;
        }
        AnalyticsIngestReceipt receipt = new AnalyticsIngestReceipt();
        receipt.setEventId(event.getEventId());
        receipt.setMatchId(event.getMatchId());
        receipt.setEventType(event.getEventType());
        receipt.setProcessedAt(Instant.now());
        try {
            receiptRepository.saveAndFlush(receipt);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent consumer already claimed this eventId after our exists check.
            return false;
        }
    }
}
