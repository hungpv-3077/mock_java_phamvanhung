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
@Schema(description = "Request DTO for Facebook login")
public class FacebookLoginRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(description = "Facebook authorization code received from Facebook OAuth", example = "AQBx7...")
    private String code;
}
