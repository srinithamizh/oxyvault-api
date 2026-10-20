package in.srinithamizh.oxyvault.security;

import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated user as seen by Spring Security.
 * <p>
 * Carries the role and token version so that login and JWT validation can work from a single
 * database lookup instead of re-querying the user.
 */
public final class UserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private final UUID id;
    @Getter
    private final String username;
    private final String password;
    @Getter
    private final String firstName;
    @Getter
    private final String lastName;
    @Getter
    private final String email;
    @Getter
    private final Role role;
    @Getter
    private final long tokenVersion;
    private final List<SimpleGrantedAuthority> authorities;

    public UserPrincipal(String username, String password, Role role, long tokenVersion, UUID id, String firstName, String lastName, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.tokenVersion = tokenVersion;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getTokenVersion(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // Deliberately excludes the password hash.
    @Override
    public String toString() {
        return "UserPrincipal[username=" + username + ", role=" + role + "]";
    }
}
