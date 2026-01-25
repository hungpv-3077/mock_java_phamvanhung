package sun.asterisk.booking_tour.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sun.asterisk.booking_tour.entity.Token;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    
    /**
     * Find token by token key (UUID)
     */
    Optional<Token> findByTokenKey(String tokenKey);
    
    /**
     * Find all tokens by user ID
     */
    List<Token> findByUserId(Long userId);
    
    /**
     * Find token by refresh token
     */
    Optional<Token> findByRefreshToken(String refreshToken);
    
    /**
     * Delete token by token key
     */
    @Modifying
    @Query("DELETE FROM Token t WHERE t.tokenKey = :tokenKey")
    int deleteByTokenKey(@Param("tokenKey") String tokenKey);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM Token t WHERE t.refreshExpiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Check if token key exists
     */
    boolean existsByTokenKey(String tokenKey);
}
