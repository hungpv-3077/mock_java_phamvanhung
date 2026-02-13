package sun.asterisk.booking_tour.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.asterisk.booking_tour.dto.email.BookingEmailMessage;
import sun.asterisk.booking_tour.entity.FailedEmailMessage;
import sun.asterisk.booking_tour.repository.FailedEmailMessageRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final FailedEmailMessageRepository failedEmailMessageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveToDeadLetterQueue(BookingEmailMessage message, String errorMessage, int retryCount) {
        try {
            log.warn("Saving message to DLQ for booking: {}, retry count: {}", 
                    message.getBookingCode(), retryCount);

            String messageContent = objectMapper.writeValueAsString(message);

            FailedEmailMessage failedMessage = failedEmailMessageRepository
                    .findByBookingCode(message.getBookingCode())
                    .orElse(new FailedEmailMessage());

            failedMessage.setBookingCode(message.getBookingCode());
            failedMessage.setContactEmail(message.getContactEmail());
            failedMessage.setMessageContent(messageContent);
            failedMessage.setErrorMessage(errorMessage);
            failedMessage.setRetryCount(retryCount);
            failedMessage.setLastRetryAt(LocalDateTime.now());

            if (failedMessage.getId() == null) {
                failedMessage.setFailedAt(LocalDateTime.now());
            }

            failedEmailMessageRepository.save(failedMessage);
            
            log.info("Successfully saved message to DLQ for booking: {}", message.getBookingCode());
        } catch (Exception e) {
            log.error("Failed to save message to DLQ for booking: {}", message.getBookingCode(), e);
        }
    }
}
