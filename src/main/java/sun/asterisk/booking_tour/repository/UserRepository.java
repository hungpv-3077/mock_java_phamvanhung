package sun.asterisk.booking_tour.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sun.asterisk.booking_tour.entity.User;
import sun.asterisk.booking_tour.enums.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role ORDER BY u.id DESC")
    List<User> findAllWithRole();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.status = :status ORDER BY u.id DESC")
    List<User> findByStatus(@Param("status") UserStatus status);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role " +
           "WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY u.id DESC")
    List<User> findBySearch(@Param("search") String search);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role " +
           "WHERE u.status = :status " +
           "AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY u.id DESC")
    List<User> findBySearchAndStatus(@Param("search") String search, @Param("status") UserStatus status);
}
