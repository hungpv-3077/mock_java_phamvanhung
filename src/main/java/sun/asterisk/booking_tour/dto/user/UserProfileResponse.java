package sun.asterisk.booking_tour.dto.user;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sun.asterisk.booking_tour.enums.UserStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile information")
public class UserProfileResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "User phone number", example = "+84123456789")
    private String phone;

    @Schema(description = "User date of birth", example = "1990-01-01")
    private LocalDate dateOfBirth;

    @Schema(description = "User avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "User verification status", example = "true")
    private Boolean isVerified;

    @Schema(description = "User status", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "User role name", example = "USER")
    private String role;
}
