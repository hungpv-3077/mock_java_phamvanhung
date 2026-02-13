package sun.asterisk.booking_tour.dto.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingNotificationDto {
    private String bookingCode;
    private String customerName;
    private String customerEmail;
    private BigDecimal amount;
    private String tourName;
    private LocalDateTime paymentDate;
    private String message;
}
