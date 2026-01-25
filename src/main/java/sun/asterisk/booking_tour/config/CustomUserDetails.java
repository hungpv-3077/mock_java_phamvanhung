package sun.asterisk.booking_tour.config;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import sun.asterisk.booking_tour.enums.UserStatus;

@Data
@AllArgsConstructor
@Builder
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private Boolean isVerified;
    private UserStatus status;
    private String roleName;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roleName != null) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
        }
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return null; // OAuth users don't have passwords
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
