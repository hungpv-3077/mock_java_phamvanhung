package sun.asterisk.booking_tour.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEmailMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String bookingCode;
    private String tourName;
    private LocalDate departureDate;
    private Integer numAdults;
    private Integer numChildren;
    private BigDecimal finalTotal;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String status;
}
