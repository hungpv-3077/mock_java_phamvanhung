package sun.asterisk.booking_tour.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sun.asterisk.booking_tour.entity.Booking;
import sun.asterisk.booking_tour.entity.Payment;
import sun.asterisk.booking_tour.enums.BookingStatus;
import sun.asterisk.booking_tour.enums.PaymentStatus;
import sun.asterisk.booking_tour.repository.BookingRepository;
import sun.asterisk.booking_tour.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingSchedulerService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Value("${booking.auto-cancel.expiration-minutes:15}")
    private int expirationMinutes;

    @Scheduled(fixedDelayString = "${booking.auto-cancel.check-interval-ms:300000}")
    public void scheduleBookingCancellation() {
        log.info("Starting scheduled task to cancel unpaid bookings");
        cancelExpiredBookings();
    }

    @Async
    @Transactional
    public void cancelExpiredBookings() {
        try {
            LocalDateTime expirationTime = LocalDateTime.now()
                    .minusMinutes(getExpirationMinutes());

            List<Booking> expiredBookings = bookingRepository.findPendingBookingsOlderThan(
                    BookingStatus.PENDING,
                    expirationTime
            );

            log.info("Found {} expired bookings to process", expiredBookings.size());

            int cancelledCount = 0;
            for (Booking booking : expiredBookings) {
                if (shouldCancelBooking(booking)) {
                    cancelBooking(booking);
                    cancelledCount++;
                }
            }

            log.info("Successfully cancelled {} bookings", cancelledCount);
        } catch (Exception e) {
            log.error("Error occurred while cancelling expired bookings", e);
        }
    }

    private boolean shouldCancelBooking(Booking booking) {
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());
        
        boolean hasSuccessfulPayment = payments.stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.COMPLETED);

        return !hasSuccessfulPayment;
    }

    private void cancelBooking(Booking booking) {
        try {
            log.info("Cancelling booking: {} (Code: {})", booking.getId(), booking.getCode());

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            if (booking.getTourDeparture() != null) {
                var departure = booking.getTourDeparture();
                int totalPeople = safeInt(booking.getNumAdults()) + safeInt(booking.getNumChildren());
                
                Integer currentSlots = departure.getAvailableSlots() != null 
                        ? departure.getAvailableSlots() 
                        : 0;
                departure.setAvailableSlots(currentSlots + totalPeople);
                
                log.info("Restored {} slots to tour departure {}", totalPeople, departure.getId());
            }

            log.info("Successfully cancelled booking: {}", booking.getCode());
        } catch (Exception e) {
            log.error("Failed to cancel booking: {}", booking.getCode(), e);
            throw e;
        }
    }

    private int getExpirationMinutes() {
        return expirationMinutes;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
