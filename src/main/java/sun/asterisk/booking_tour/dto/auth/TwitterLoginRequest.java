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
@Schema(description = "Request DTO for Twitter login")
public class TwitterLoginRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(description = "Twitter authorization code received from Twitter OAuth", example = "VGhpcyBpcyBh...")
    private String code;
}
