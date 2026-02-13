package sun.asterisk.booking_tour.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sun.asterisk.booking_tour.entity.FailedEmailMessage;

import java.util.Optional;

@Repository
public interface FailedEmailMessageRepository extends JpaRepository<FailedEmailMessage, Long> {
    Optional<FailedEmailMessage> findByBookingCode(String bookingCode);
}
