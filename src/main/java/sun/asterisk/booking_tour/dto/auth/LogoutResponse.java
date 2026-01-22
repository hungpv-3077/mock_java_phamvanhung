package sun.asterisk.booking_tour.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response for logout operation")
public class LogoutResponse {

    @Schema(description = "Success message", example = "Logout successful")
    private String message;

    @Schema(description = "Timestamp of logout", example = "2026-01-22T13:45:00")
    private String timestamp;
}
