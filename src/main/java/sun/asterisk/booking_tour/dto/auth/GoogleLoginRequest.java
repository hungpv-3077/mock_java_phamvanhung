package sun.asterisk.booking_tour.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for Google login")
public class GoogleLoginRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(description = "Google authorization code received from Google OAuth", example = "4/0AY0e-g7...")
    private String code;
}
