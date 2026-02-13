package sun.asterisk.booking_tour.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sun.asterisk.booking_tour.entity.Booking;
import sun.asterisk.booking_tour.enums.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByCode(String code);

    @Query("select b from Booking b "
            + "join fetch b.tourDeparture td "
            + "join fetch td.tour t "
            + "where b.code = :code")
    Optional<Booking> findByCodeWithDepartureAndTour(@Param("code") String code);

    boolean existsByCode(String code);

    // Revenue queries
    @Query("SELECT COALESCE(SUM(b.finalTotal), 0) FROM Booking b " +
           "WHERE b.status = :status " +
           "AND b.createdAt >= :startDate " +
           "AND b.createdAt < :endDate")
    BigDecimal calculateRevenue(
        @Param("status") BookingStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.status = :status " +
           "AND b.createdAt >= :startDate " +
           "AND b.createdAt < :endDate")
    Long countBookings(
        @Param("status") BookingStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t.id as tourId, t.name as tourName, " +
           "COUNT(b) as bookingCount, COALESCE(SUM(b.finalTotal), 0) as totalRevenue " +
           "FROM Booking b " +
           "JOIN b.tourDeparture td " +
           "JOIN td.tour t " +
           "WHERE b.status = :status " +
           "AND b.createdAt >= :startDate " +
           "AND b.createdAt < :endDate " +
           "GROUP BY t.id, t.name " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> findTourRevenueStats(
        @Param("status") BookingStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT YEAR(b.createdAt) as year, MONTH(b.createdAt) as month, " +
           "COALESCE(SUM(b.finalTotal), 0) as revenue, COUNT(b) as bookingCount " +
           "FROM Booking b " +
           "WHERE b.status = :status " +
           "AND b.createdAt >= :startDate " +
           "GROUP BY YEAR(b.createdAt), MONTH(b.createdAt) " +
           "ORDER BY year, month")
    List<Object[]> findMonthlyRevenue(
        @Param("status") BookingStatus status,
        @Param("startDate") LocalDateTime startDate
    );

    // Admin booking queries
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.tourDeparture td " +
           "JOIN FETCH td.tour t " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findAllWithTourInfo();

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.tourDeparture td " +
           "JOIN FETCH td.tour t " +
           "WHERE b.status = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByStatusWithTourInfo(@Param("status") BookingStatus status);

    @EntityGraph(attributePaths = { "tourDeparture", "tourDeparture.tour" })
    Page<Booking> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "tourDeparture", "tourDeparture.tour" })
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "tourDeparture", "tourDeparture.tour" })
    Page<Booking> findByStatusNot(BookingStatus status, Pageable pageable);

    Long countByStatus(BookingStatus status);

    // Auto-cancellation queries
    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.tourDeparture td " +
           "WHERE b.status = :status " +
           "AND b.createdAt < :expirationTime")
    List<Booking> findPendingBookingsOlderThan(
        @Param("status") BookingStatus status,
        @Param("expirationTime") LocalDateTime expirationTime
    );
}
