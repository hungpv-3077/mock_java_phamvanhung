package sun.asterisk.booking_tour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import sun.asterisk.booking_tour.dto.email.BookingEmailMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailMessageProducer {

    private final JmsTemplate jmsTemplate;

    @Value("${jms.queue.email}")
    private String emailQueue;

    public void sendBookingEmail(BookingEmailMessage message) {
        try {
            log.info("Sending booking email message to JMS queue for booking: {}", message.getBookingCode());
            jmsTemplate.convertAndSend(emailQueue, message, msg -> {
                msg.setIntProperty("retry-count", 0);
                return msg;
            });
            log.info("Successfully sent booking email message to JMS queue for booking: {}", message.getBookingCode());
        } catch (Exception e) {
            log.error("Failed to send booking email message to JMS queue for booking: {}", 
                    message.getBookingCode(), e);
            throw new RuntimeException("Failed to send email message", e);
        }
    }
}
