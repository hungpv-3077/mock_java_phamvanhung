package sun.asterisk.booking_tour.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Add token to blacklist with TTL
     *
     * @param tokenKey UUID token key
     * @param ttlSeconds Time to live in seconds (should match JWT expiration)
     */
    public void blacklistToken(String tokenKey, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + tokenKey;
        redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
        logger.info("Token blacklisted: {} for {} seconds", tokenKey, ttlSeconds);
    }

    /**
     * Check if token is blacklisted
     *
     * @param tokenKey UUID token key
     * @return true if blacklisted, false otherwise
     */
    public boolean isBlacklisted(String tokenKey) {
        String key = BLACKLIST_PREFIX + tokenKey;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Remove token from blacklist (optional, mainly for testing)
     *
     * @param tokenKey UUID token key
     */
    public void removeFromBlacklist(String tokenKey) {
        String key = BLACKLIST_PREFIX + tokenKey;
        redisTemplate.delete(key);
        logger.info("Token removed from blacklist: {}", tokenKey);
    }
}
