package sun.asterisk.booking_tour.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import sun.asterisk.booking_tour.dto.notification.BookingNotificationDto;
import sun.asterisk.booking_tour.entity.Booking;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendBookingSuccessNotification(Booking booking) {
        try {
            String tourName = "N/A";
            if (booking.getTourDeparture() != null && booking.getTourDeparture().getTour() != null) {
                tourName = booking.getTourDeparture().getTour().getName();
            }

            BookingNotificationDto notification = BookingNotificationDto.builder()
                    .bookingCode(booking.getCode())
                    .customerName(booking.getContactName())
                    .customerEmail(booking.getContactEmail())
                    .amount(booking.getFinalTotal())
                    .tourName(tourName)
                    .paymentDate(java.time.LocalDateTime.now())
                    .message("New booking payment completed successfully!")
                    .build();

            messagingTemplate.convertAndSend("/topic/admin/bookings", notification);
            logger.info("Sent booking notification to admin: {}", booking.getCode());
        } catch (Exception e) {
            logger.error("Failed to send booking notification for: {}", booking.getCode(), e);
        }
    }
}
