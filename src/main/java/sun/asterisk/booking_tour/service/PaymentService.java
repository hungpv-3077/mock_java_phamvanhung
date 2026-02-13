package sun.asterisk.booking_tour.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sun.asterisk.booking_tour.config.StripeProperties;
import sun.asterisk.booking_tour.dto.payment.StripeCheckoutResponse;
import sun.asterisk.booking_tour.dto.payment.StripePaymentStatusResponse;
import sun.asterisk.booking_tour.entity.Booking;
import sun.asterisk.booking_tour.entity.Payment;
import sun.asterisk.booking_tour.enums.BookingStatus;
import sun.asterisk.booking_tour.enums.PaymentMethod;
import sun.asterisk.booking_tour.enums.PaymentStatus;
import sun.asterisk.booking_tour.exception.ResourceNotFoundException;
import sun.asterisk.booking_tour.exception.ValidationException;
import sun.asterisk.booking_tour.repository.BookingRepository;
import sun.asterisk.booking_tour.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    private final StripeProperties stripeProperties;

    private final EmailQueueService emailQueueService;

    private final NotificationService notificationService;

    public PaymentService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            StripeProperties stripeProperties,
            EmailQueueService emailQueueService,
            NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.stripeProperties = stripeProperties;
        this.emailQueueService = emailQueueService;
        this.notificationService = notificationService;
    }

    @Transactional
    public StripeCheckoutResponse createStripeCheckout(String bookingCode) {
        Booking booking = bookingRepository.findByCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        BigDecimal amount = booking.getFinalTotal();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Invalid booking amount");
        }
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new ValidationException("Missing STRIPE_SECRET_KEY");
        }

        Stripe.apiKey = stripeProperties.getSecretKey();

        long unitAmount = amount.movePointRight(2).longValue();
        if (unitAmount <= 0) {
            throw new ValidationException("Invalid booking amount");
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(stripeProperties.getSuccessUrl())
                    .setCancelUrl(stripeProperties.getCancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(stripeProperties.getCurrency())
                                                    .setUnitAmount(unitAmount)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Booking " + booking.getCode())
                                                                    .build())
                                                    .build())
                                    .build())
                    .putMetadata("bookingCode", booking.getCode())
                    .build();
            Session session = Session.create(params);

            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(amount);
            payment.setPaymentMethod(PaymentMethod.STRIPE);
            payment.setTransactionId(session.getId());
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            return new StripeCheckoutResponse(session.getId(), session.getUrl());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create Stripe checkout session", ex);
        }
    }

    @Transactional
    public StripePaymentStatusResponse handleStripeSuccess(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ValidationException("Missing session_id");
        }
        if (stripeProperties.getSecretKey() == null || stripeProperties.getSecretKey().isBlank()) {
            throw new ValidationException("Missing STRIPE_SECRET_KEY");
        }

        Stripe.apiKey = stripeProperties.getSecretKey();

        try {
            Session session = Session.retrieve(sessionId);
            Payment payment = paymentRepository.findByTransactionId(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                return new StripePaymentStatusResponse(true, "Payment already completed",
                        payment.getBooking() != null ? payment.getBooking().getCode() : null, sessionId);
            }

            boolean paid = "paid".equalsIgnoreCase(session.getPaymentStatus());
            if (!paid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return new StripePaymentStatusResponse(false, "Payment not completed",
                        payment.getBooking() != null ? payment.getBooking().getCode() : null, sessionId);
            }

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);

            Booking booking = payment.getBooking();
            if (booking != null) {
                booking.setStatus(BookingStatus.PAID);
                bookingRepository.save(booking);

                String bookingCode = booking.getCode();
                String toEmail = booking.getContactEmail();
                try {
                    emailQueueService.enqueueBookingPaymentSuccess(booking);
                    logger.warn("Enqueued payment success email job. bookingCode={}, to={}", bookingCode, toEmail);
                } catch (Exception e) {
                    logger.error("Failed to enqueue payment success email job. bookingCode={}, to={}", bookingCode, toEmail, e);
                }

                // Send WebSocket notification to admin
                try {
                    notificationService.sendBookingSuccessNotification(booking);
                } catch (Exception e) {
                    logger.error("Failed to send WebSocket notification for booking: {}", bookingCode, e);
                }
            }

            return new StripePaymentStatusResponse(true, "Payment success",
                    booking != null ? booking.getCode() : null, sessionId);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to verify Stripe session", ex);
        }
    }

    @Transactional
    public StripePaymentStatusResponse handleStripeCancel(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ValidationException("Missing session_id");
        }

        Payment payment = paymentRepository.findByTransactionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        Booking booking = payment.getBooking();
        return new StripePaymentStatusResponse(false, "Payment cancelled",
                booking != null ? booking.getCode() : null, sessionId);
    }
}
