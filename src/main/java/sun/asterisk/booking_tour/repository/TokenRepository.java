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
     * Find token by token key and not revoked
     */
    Optional<Token> findByTokenKeyAndIsRevokedFalse(String tokenKey);
    
    /**
     * Find all tokens by user ID and not revoked
     */
    List<Token> findByUserIdAndIsRevokedFalse(Long userId);
    
    /**
     * Find token by refresh token and not revoked
     */
    Optional<Token> findByRefreshTokenAndIsRevokedFalse(String refreshToken);
    
    /**
     * Revoke (soft delete) token by token key
     */
    @Modifying
    @Query("UPDATE Token t SET t.isRevoked = true, t.revokedAt = :revokedAt WHERE t.tokenKey = :tokenKey")
    int revokeByTokenKey(@Param("tokenKey") String tokenKey, @Param("revokedAt") LocalDateTime revokedAt);
    
    /**
     * Revoke all tokens for a user
     */
    @Modifying
    @Query("UPDATE Token t SET t.isRevoked = true, t.revokedAt = :revokedAt WHERE t.user.id = :userId AND t.isRevoked = false")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);
    
    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM Token t WHERE t.refreshExpiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Check if token key exists and is not revoked
     */
    boolean existsByTokenKeyAndIsRevokedFalse(String tokenKey);
}
