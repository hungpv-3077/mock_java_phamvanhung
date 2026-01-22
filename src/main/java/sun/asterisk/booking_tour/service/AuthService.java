package sun.asterisk.booking_tour.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sun.asterisk.booking_tour.config.JwtTokenProvider;
import sun.asterisk.booking_tour.dto.auth.AuthResponse;
import sun.asterisk.booking_tour.dto.auth.UserInfo;
import sun.asterisk.booking_tour.dto.auth.UserResponse;
import sun.asterisk.booking_tour.entity.Role;
import sun.asterisk.booking_tour.entity.Token;
import sun.asterisk.booking_tour.entity.User;
import sun.asterisk.booking_tour.enums.UserStatus;
import sun.asterisk.booking_tour.exception.ValidationException;
import sun.asterisk.booking_tour.repository.RoleRepository;
import sun.asterisk.booking_tour.repository.TokenRepository;
import sun.asterisk.booking_tour.repository.UserRepository;
import sun.asterisk.booking_tour.enums.AuthProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient.Builder webClientBuilder;
    private final RoleRepository roleRepository;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${oauth.facebook.client-id}")
    private String facebookClientId;

    @Value("${oauth.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${oauth.facebook.redirect-uri}")
    private String facebookRedirectUri;

    @Value("${oauth.twitter.client-id}")
    private String twitterClientId;

    @Value("${oauth.twitter.client-secret}")
    private String twitterClientSecret;

    @Value("${oauth.twitter.redirect-uri}")
    private String twitterRedirectUri;

    /**
     * Login with Google authorization code
     */
    @Transactional
    public AuthResponse loginWithGoogle(String code) {
        try {
            String accessToken = exchangeGoogleCodeForToken(code);
            UserInfo userInfo = getGoogleUserInfo(accessToken);
            User user = findOrCreateUser(userInfo, AuthProvider.GOOGLE.name());

            return generateAuthResponse(user);

        } catch (Exception e) {
            log.error("Error verifying Google code", e);
            throw new ValidationException("Invalid Google authorization code");
        }
    }

    /**
     * Login with Facebook authorization code
     */
    @Transactional
    public AuthResponse loginWithFacebook(String code) {
        try {
            String accessToken = exchangeFacebookCodeForToken(code);
            UserInfo userInfo = getFacebookUserInfo(accessToken);

            User user = findOrCreateUser(userInfo, AuthProvider.FACEBOOK.name());

            return generateAuthResponse(user);

        } catch (Exception e) {
            log.error("Error verifying Facebook code", e);
            throw new ValidationException("Invalid Facebook authorization code");
        }
    }

    /**
     * Login with Twitter authorization code
     */
    @Transactional
    public AuthResponse loginWithTwitter(String code) {
        try {
            String accessToken = exchangeTwitterCodeForToken(code);
            UserInfo userInfo = getTwitterUserInfo(accessToken);

            User user = findOrCreateUser(userInfo, AuthProvider.TWITTER.name());

            return generateAuthResponse(user);

        } catch (Exception e) {
            log.error("Error verifying Twitter code", e);
            throw new ValidationException("Invalid Twitter authorization code");
        }
    }

    /**
     * Logout user by revoking token
     */
    @Transactional
    public void logout(String accessToken) {
        try {
            if (!jwtTokenProvider.validateToken(accessToken)) {
                throw new ValidationException("Invalid or expired token");
            }

            String tokenKey = jwtTokenProvider.getTokenKeyFromToken(accessToken);

            if (tokenKey == null || tokenKey.isEmpty()) {
                throw new ValidationException("Token key not found in token payload");
            }

            int revokedCount = tokenRepository.revokeByTokenKey(tokenKey, LocalDateTime.now());

            if (revokedCount == 0) {
                log.warn("Token key not found in database or already revoked: {}", tokenKey);
                throw new ValidationException("Token not found or already revoked");
            }

            log.info("Successfully revoked token with key: {}", tokenKey);

        } catch (Exception e) {
            log.error("Error during logout", e);
            if (e instanceof ValidationException) {
                throw e;
            }
            throw new ValidationException("Logout failed: " + e.getMessage());
        }
    }

    /**
     * Exchange Google authorization code for access token
     */
    private String exchangeGoogleCodeForToken(String code) {
        WebClient webClient = webClientBuilder.build();

        GoogleTokenResponse response = webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(String.format(
                        "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
                        code, googleRedirectUri, googleClientId, googleClientSecret))
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();

        if (response == null || response.getAccessToken() == null) {
            throw new ValidationException("Failed to exchange Google code for token");
        }

        return response.getAccessToken();
    }

    /**
     * Get Google user information using access token
     */
    private UserInfo getGoogleUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.build();

        GoogleUserInfo response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("www.googleapis.com")
                .path("/oauth2/v2/userinfo")
                .queryParam("access_token", accessToken)
                .build())
                .retrieve()
                .bodyToMono(GoogleUserInfo.class)
                .block();

        if (response == null || response.getEmail() == null) {
            throw new ValidationException("Failed to get Google user info");
        }

        return UserInfo.builder()
                .email(response.getEmail())
                .firstName(response.getGivenName())
                .lastName(response.getFamilyName())
                .avatarUrl(response.getPicture())
                .provider("GOOGLE")
                .build();
    }

    /**
     * Exchange Facebook authorization code for access token
     */
    private String exchangeFacebookCodeForToken(String code) {
        WebClient webClient = webClientBuilder.build();
        System.out.println("Exchanging Facebook code for token with code: " + code);
        System.out.println("Facebook Client ID: " + facebookClientId);
        System.out.println("Facebook Client Secret: " + facebookClientSecret);
        System.out.println("Facebook Redirect URI: " + facebookRedirectUri);

        FacebookTokenResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("graph.facebook.com")
                .path("/v19.0/oauth/access_token")
                .queryParam("client_id", facebookClientId)
                .queryParam("client_secret", facebookClientSecret)
                .queryParam("redirect_uri", facebookRedirectUri)
                .queryParam("code", code)
                .build())
                .retrieve()
                .bodyToMono(FacebookTokenResponse.class)
                .block();

        System.out.println("Facebook Token Response: " + response);

        if (response == null || response.getAccessToken() == null) {
            throw new ValidationException("Failed to exchange Facebook code for token");
        }

        return response.getAccessToken();
    }

    /**
     * Get Facebook user information using access token
     */
    private UserInfo getFacebookUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.build();

        FacebookUserInfo response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("graph.facebook.com")
                .path("/me")
                .queryParam("fields", "id,email,first_name,last_name,picture")
                .queryParam("access_token", accessToken)
                .build())
                .retrieve()
                .bodyToMono(FacebookUserInfo.class)
                .block();

        if (response == null || response.getEmail() == null) {
            throw new ValidationException("Failed to get user info from Facebook");
        }

        return UserInfo.builder()
                .email(response.getEmail())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .avatarUrl(response.getPicture() != null ? response.getPicture().getData().getUrl() : null)
                .provider("FACEBOOK")
                .build();
    }

    /**
     * Exchange Twitter authorization code for access token
     */
    private String exchangeTwitterCodeForToken(String code) {
        WebClient webClient = webClientBuilder.build();

        TwitterTokenResponse response = webClient.post()
                .uri("https://api.twitter.com/2/oauth2/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(String.format(
                        "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
                        code, twitterRedirectUri, twitterClientId, twitterClientSecret))
                .retrieve()
                .bodyToMono(TwitterTokenResponse.class)
                .block();

        if (response == null || response.getAccessToken() == null) {
            throw new ValidationException("Failed to exchange Twitter code for token");
        }

        return response.getAccessToken();
    }

    /**
     * Get Twitter user information using access token
     */
    private UserInfo getTwitterUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.build();

        TwitterUserResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host("api.twitter.com")
                .path("/2/users/me")
                .queryParam("user.fields", "profile_image_url")
                .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(TwitterUserResponse.class)
                .block();

        if (response == null || response.getData() == null) {
            throw new ValidationException("Failed to get user info from Twitter");
        }

        TwitterUserData userData = response.getData();
        String[] nameParts = userData.getName() != null ? userData.getName().split(" ", 2) : new String[]{"", ""};

        return UserInfo.builder()
                .email(userData.getUsername() + "@twitter.oauth") // Twitter doesn't provide email in v2 API
                .firstName(nameParts.length > 0 ? nameParts[0] : "")
                .lastName(nameParts.length > 1 ? nameParts[1] : "")
                .avatarUrl(userData.getProfileImageUrl())
                .provider("TWITTER")
                .build();
    }

    /**
     * Find existing user or create new one
     */
    private User findOrCreateUser(UserInfo userInfo, String provider) {
        return userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> {
                    Role userRole = roleRepository.findByName("USER")
                            .orElseThrow(() -> new ValidationException("Default USER role not found. Please run data seeding."));

                    User newUser = new User();
                    newUser.setEmail(userInfo.getEmail());
                    newUser.setFirstName(userInfo.getFirstName());
                    newUser.setLastName(userInfo.getLastName());
                    newUser.setAvatarUrl(userInfo.getAvatarUrl());
                    newUser.setPassword("");
                    newUser.setIsVerified(true);
                    newUser.setStatus(UserStatus.ACTIVE);
                    newUser.setRole(userRole);

                    User savedUser = userRepository.save(newUser);
                    log.info("Created new user with email: {} and assigned role: {}", savedUser.getEmail(), userRole.getName());

                    return savedUser;
                });
    }

    /**
     * Generate authentication response with JWT tokens and save to database
     */
    private AuthResponse generateAuthResponse(User user) {
        // Generate unique token key (UUID)
        String tokenKey = jwtTokenProvider.generateTokenKey();

        // Generate JWT tokens with token key in payload
        String accessToken = jwtTokenProvider.generateAccessToken(user, tokenKey);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, tokenKey);

        // Calculate expiration times
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessExpiresAt = now.plusSeconds(jwtTokenProvider.getJwtExpiration());
        LocalDateTime refreshExpiresAt = now.plusSeconds(jwtTokenProvider.getRefreshExpiration());

        // Save token to database
        Token token = Token.builder()
                .tokenKey(tokenKey)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .expiresAt(accessExpiresAt)
                .refreshExpiresAt(refreshExpiresAt)
                .isRevoked(false)
                .build();

        tokenRepository.save(token);

        log.info("Generated and saved new token for user: {} with key: {}", user.getEmail(), tokenKey);

        // Build user response
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .build();

        return AuthResponse.builder()
                .users(userResponse)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpiration())
                .build();
    }

    // Inner classes for API responses
    @lombok.Data
    private static class GoogleTokenResponse {

        @com.fasterxml.jackson.annotation.JsonProperty("access_token")
        private String accessToken;
        @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
        private Integer expiresIn;
        @com.fasterxml.jackson.annotation.JsonProperty("token_type")
        private String tokenType;
    }

    @lombok.Data
    private static class GoogleUserInfo {

        private String id;
        private String email;
        @com.fasterxml.jackson.annotation.JsonProperty("given_name")
        private String givenName;
        @com.fasterxml.jackson.annotation.JsonProperty("family_name")
        private String familyName;
        private String picture;
        @com.fasterxml.jackson.annotation.JsonProperty("verified_email")
        private Boolean verifiedEmail;
    }

    @lombok.Data
    private static class FacebookTokenResponse {

        @com.fasterxml.jackson.annotation.JsonProperty("access_token")
        private String accessToken;
        @com.fasterxml.jackson.annotation.JsonProperty("token_type")
        private String tokenType;
        @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
        private Long expiresIn;
    }

    @lombok.Data
    private static class FacebookUserInfo {

        private String id;
        private String email;
        @com.fasterxml.jackson.annotation.JsonProperty("first_name")
        private String firstName;
        @com.fasterxml.jackson.annotation.JsonProperty("last_name")
        private String lastName;
        private Picture picture;

        @lombok.Data
        private static class Picture {

            private PictureData data;

            @lombok.Data
            private static class PictureData {

                private String url;
            }
        }
    }

    @lombok.Data
    private static class TwitterTokenResponse {

        @com.fasterxml.jackson.annotation.JsonProperty("access_token")
        private String accessToken;
        @com.fasterxml.jackson.annotation.JsonProperty("token_type")
        private String tokenType;
        @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
        private Long expiresIn;
        private String scope;
    }

    @lombok.Data
    private static class TwitterUserResponse {

        private TwitterUserData data;
    }

    @lombok.Data
    private static class TwitterUserData {

        private String id;
        private String name;
        private String username;
        @com.fasterxml.jackson.annotation.JsonProperty("profile_image_url")
        private String profileImageUrl;
    }
}
