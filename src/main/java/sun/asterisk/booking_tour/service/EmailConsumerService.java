package sun.asterisk.booking_tour.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import sun.asterisk.booking_tour.dto.email.BookingEmailMessage;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final JavaMailSender mailSender;
    private final DeadLetterQueueService deadLetterQueueService;

    @Value("${spring.mail.from:noreply@bookingtour.com}")
    private String from;

    @JmsListener(destination = "${jms.queue.email}")
    public void consumeBookingEmail(BookingEmailMessage message, 
                                   @Header(value = "retry-count", required = false) Integer retryCount) {
        int currentRetry = retryCount != null ? retryCount : 0;
        
        log.info("Received booking email message from JMS queue for booking: {} (retry: {}/{})", 
                message.getBookingCode(), currentRetry, MAX_RETRY_ATTEMPTS);
        
        try {
            sendBookingConfirmationEmail(message);
            log.info("Successfully sent booking confirmation email for booking: {}", message.getBookingCode());
        } catch (Exception e) {
            currentRetry++;
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            
            log.error("Failed to send booking confirmation email for booking: {} (attempt {}/{}): {}", 
                    message.getBookingCode(), currentRetry, MAX_RETRY_ATTEMPTS, errorMessage, e);
            
            if (currentRetry >= MAX_RETRY_ATTEMPTS) {
                log.error("Max retry attempts reached for booking: {}. Moving to DLQ.", message.getBookingCode());
                deadLetterQueueService.saveToDeadLetterQueue(message, errorMessage, currentRetry);
            } else {
                log.error("Retrying to send email for booking: {}. Attempt {}/{}", 
                        message.getBookingCode(), currentRetry, MAX_RETRY_ATTEMPTS);
                throw new RuntimeException("Email sending failed, will retry. Attempt " + currentRetry, e);
            }
        }
    }

    private void sendBookingConfirmationEmail(BookingEmailMessage message) {
        if (message.getContactEmail() == null || message.getContactEmail().isBlank()) {
            log.warn("Skip sending email: contactEmail is blank. bookingCode={}", message.getBookingCode());
            return;
        }

        String subject = "Booking Confirmation - " + message.getBookingCode();

        String departureDate = message.getDepartureDate() != null
                ? message.getDepartureDate().format(DATE_FORMAT)
                : "N/A";

        StringBuilder text = new StringBuilder();
        text.append("Dear ").append(message.getContactName()).append(",\n\n");
        text.append("Thank you for your booking!\n\n");
        text.append("Booking Details:\n");
        text.append("--------------------------------\n");
        text.append("Booking Code: ").append(message.getBookingCode()).append("\n");
        text.append("Tour: ").append(message.getTourName()).append("\n");
        text.append("Departure Date: ").append(departureDate).append("\n");
        text.append("Passengers: ")
                .append(message.getNumAdults() != null ? message.getNumAdults() : 0)
                .append(" adults, ")
                .append(message.getNumChildren() != null ? message.getNumChildren() : 0)
                .append(" children\n");
        text.append("Total Amount: $").append(message.getFinalTotal()).append("\n");
        text.append("Status: ").append(message.getStatus()).append("\n");
        text.append("--------------------------------\n\n");
        text.append("Contact Information:\n");
        text.append("Name: ").append(message.getContactName()).append("\n");
        text.append("Email: ").append(message.getContactEmail()).append("\n");
        text.append("Phone: ").append(message.getContactPhone()).append("\n\n");
        text.append("We will contact you soon to confirm your booking.\n\n");
        text.append("Thank you for choosing our service!\n\n");
        text.append("Best regards,\n");
        text.append("Booking Tour Team");

        SimpleMailMessage emailMessage = new SimpleMailMessage();
        
        String fromEmail = (from == null || from.isBlank()) ? "noreply@bookingtour.com" : from;
        emailMessage.setFrom(fromEmail);
        emailMessage.setTo(message.getContactEmail());
        emailMessage.setSubject(subject);
        emailMessage.setText(text.toString());

        log.info("Sending email from: {}, to: {}, subject: {}", fromEmail, message.getContactEmail(), subject);
        mailSender.send(emailMessage);
    }
}
