package sun.asterisk.booking_tour.controller.client;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sun.asterisk.booking_tour.dto.auth.AuthResponse;
import sun.asterisk.booking_tour.dto.auth.FacebookLoginRequest;
import sun.asterisk.booking_tour.dto.auth.GoogleLoginRequest;
import sun.asterisk.booking_tour.dto.auth.LogoutResponse;
import sun.asterisk.booking_tour.dto.auth.TwitterLoginRequest;
import sun.asterisk.booking_tour.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "API endpoints for OAuth authentication (Google, Facebook, Twitter)")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Login with Google",
        description = "Authenticate user using Google authorization code. The client must obtain the authorization code from Google OAuth 2.0 flow and send it to this endpoint. The server will exchange the code for an access token, verify it with Google, retrieve user information, create or update the user, and return JWT access and refresh tokens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated with Google",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid Google authorization code or validation error",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during authentication",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/google/login")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request) {
        
        AuthResponse response = authService.loginWithGoogle(request.getCode());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Login with Facebook",
        description = "Authenticate user using Facebook authorization code. The client must obtain the authorization code from Facebook OAuth flow and send it to this endpoint. The server will exchange the code for an access token, verify it with Facebook, retrieve user information, create or update the user, and return JWT access and refresh tokens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated with Facebook",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid Facebook authorization code or validation error",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during authentication",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/facebook/login")
    public ResponseEntity<AuthResponse> loginWithFacebook(
            @Valid @RequestBody FacebookLoginRequest request) {
        
        AuthResponse response = authService.loginWithFacebook(request.getCode());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Login with Twitter",
        description = "Authenticate user using Twitter authorization code. The client must obtain the authorization code from Twitter OAuth 2.0 flow and send it to this endpoint. The server will exchange the code for an access token, verify it with Twitter, retrieve user information, create or update the user, and return JWT access and refresh tokens. Note: Twitter API v2 does not provide email by default, so we use username@twitter.oauth as email."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully authenticated with Twitter",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid Twitter authorization code or validation error",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during authentication",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/twitter/login")
    public ResponseEntity<AuthResponse> loginWithTwitter(
            @Valid @RequestBody TwitterLoginRequest request) {
        
        AuthResponse response = authService.loginWithTwitter(request.getCode());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Logout",
        description = "Logout the current user by revoking the access token. The token key from the JWT payload will be used to revoke both access and refresh tokens from the database. This ensures that the tokens cannot be used again."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully logged out",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LogoutResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid or missing authorization token",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - token is invalid or expired",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(LogoutResponse.builder()
                            .message("Invalid authorization header format")
                            .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                            .build());
        }
        
        String token = authHeader.substring(7);
        authService.logout(token);
        
        return ResponseEntity.ok(LogoutResponse.builder()
                .message("Logout successful")
                .timestamp(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build());
    }
}
