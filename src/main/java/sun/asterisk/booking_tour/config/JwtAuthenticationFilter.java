package sun.asterisk.booking_tour.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sun.asterisk.booking_tour.entity.User;
import sun.asterisk.booking_tour.repository.UserRepository;
import sun.asterisk.booking_tour.service.TokenBlacklistService;

import java.io.IOException;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String tokenKey = jwtTokenProvider.getTokenKeyFromToken(token);
            
            if (tokenBlacklistService.isBlacklisted(tokenKey)) {
                logger.warn("Token has been revoked: {}", tokenKey);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                return;
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            Optional<User> userOptional = userRepository.findByIdWithRole(userId);
            
            if (!userOptional.isPresent()) {
                logger.warn("User not found with ID: {}", userId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                return;
            }

            User user = userOptional.get();
            CustomUserDetails userDetails = CustomUserDetails.builder()
                    .userId(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .dateOfBirth(user.getDateOfBirth())
                    .avatarUrl(user.getAvatarUrl())
                    .isVerified(user.getIsVerified())
                    .status(user.getStatus())
                    .roleName(user.getRole() != null ? user.getRole().getName() : null)
                    .build();

            UsernamePasswordAuthenticationToken authentication
                    = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
