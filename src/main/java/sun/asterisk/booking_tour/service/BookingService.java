package sun.asterisk.booking_tour.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sun.asterisk.booking_tour.dto.booking.CreateBookingRequest;
import sun.asterisk.booking_tour.dto.booking.CreateBookingResponse;
import sun.asterisk.booking_tour.dto.email.BookingEmailMessage;
import sun.asterisk.booking_tour.entity.Booking;
import sun.asterisk.booking_tour.entity.Tour;
import sun.asterisk.booking_tour.entity.TourDeparture;
import sun.asterisk.booking_tour.enums.BookingStatus;
import sun.asterisk.booking_tour.exception.ResourceNotFoundException;
import sun.asterisk.booking_tour.exception.ValidationException;
import sun.asterisk.booking_tour.repository.BookingRepository;
import sun.asterisk.booking_tour.repository.TourDepartureRepository;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BookingRepository bookingRepository;
    private final TourDepartureRepository tourDepartureRepository;
    private final EmailMessageProducer emailMessageProducer;

    @Transactional
    public CreateBookingResponse createBooking(CreateBookingRequest request) {
        int totalPeople = safeInt(request.getNumAdults()) + safeInt(request.getNumChildren());
        if (totalPeople <= 0) {
            throw new ValidationException("Number of passengers must be greater than 0");
        }

        TourDeparture departure = tourDepartureRepository.findById(request.getTourDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException("Tour departure not found"));

        if (departure.getAvailableSlots() == null || departure.getAvailableSlots() < totalPeople) {
            throw new ValidationException("Not enough available slots");
        }

        departure.setAvailableSlots(departure.getAvailableSlots() - totalPeople);

        Tour tour = departure.getTour();
        BigDecimal priceAdult = tour != null && tour.getPriceAdult() != null ? tour.getPriceAdult() : BigDecimal.ZERO;
        BigDecimal priceChild = tour != null && tour.getPriceChild() != null ? tour.getPriceChild() : BigDecimal.ZERO;

        BigDecimal subTotal = priceAdult
                .multiply(BigDecimal.valueOf(safeInt(request.getNumAdults())))
                .add(priceChild.multiply(BigDecimal.valueOf(safeInt(request.getNumChildren()))));

        BigDecimal discountRate = tour != null ? tour.getDiscountRate() : null;
        BigDecimal discount = calculateDiscount(subTotal, discountRate);
        BigDecimal finalTotal = subTotal.subtract(discount);

        Booking booking = new Booking();
        booking.setTourDeparture(departure);
        booking.setCode(generateBookingCode());
        booking.setStatus(BookingStatus.PENDING);
        booking.setNumAdults(safeInt(request.getNumAdults()));
        booking.setNumChildren(safeInt(request.getNumChildren()));
        booking.setSubTotal(subTotal);
        booking.setDiscount(discount);
        booking.setFinalTotal(finalTotal);
        booking.setContactName(request.getContactName());
        booking.setContactEmail(request.getContactEmail());
        booking.setContactPhone(request.getContactPhone());
        booking.setNotes(request.getNotes());

        bookingRepository.save(booking);

        // Send email notification via RabbitMQ
        sendBookingEmailNotification(booking);

        return CreateBookingResponse.builder()
                .code(booking.getCode())
                .status(booking.getStatus())
                .finalTotal(booking.getFinalTotal())
                .build();
    }

    private void sendBookingEmailNotification(Booking booking) {
        try {
            TourDeparture departure = booking.getTourDeparture();
            Tour tour = departure != null ? departure.getTour() : null;
            String tourName = tour != null && tour.getName() != null ? tour.getName() : "Unknown Tour";

            BookingEmailMessage emailMessage = BookingEmailMessage.builder()
                    .bookingCode(booking.getCode())
                    .tourName(tourName)
                    .departureDate(departure != null ? departure.getDepartureDate() : null)
                    .numAdults(booking.getNumAdults())
                    .numChildren(booking.getNumChildren())
                    .finalTotal(booking.getFinalTotal())
                    .contactName(booking.getContactName())
                    .contactEmail(booking.getContactEmail())
                    .contactPhone(booking.getContactPhone())
                    .status(booking.getStatus().name())
                    .build();

            emailMessageProducer.sendBookingEmail(emailMessage);
        } catch (Exception e) {
            System.err.println("Failed to send booking email notification: " + e.getMessage());
            throw new RuntimeException("Failed to send booking email notification", e);
        }
    }

    private String generateBookingCode() {
        String code;
        do {
            String timestamp = LocalDateTime.now().format(CODE_DATE_FORMAT);
            int suffix = RANDOM.nextInt(900000) + 100000;
            code = "BK" + timestamp + suffix;
        } while (bookingRepository.existsByCode(code));
        return code;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal calculateDiscount(BigDecimal subTotal, BigDecimal discountRate) {
        if (subTotal == null) {
            return BigDecimal.ZERO;
        }
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return subTotal
                .multiply(discountRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
