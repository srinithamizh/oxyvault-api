package io.github.srinithamizh.oxyvault.security;

import io.github.srinithamizh.oxyvault.entity.User;
import io.github.srinithamizh.oxyvault.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Authenticated user as seen by Spring Security.
 * <p>
 * Carries the role and token version so that login and JWT validation can work from a single
 * database lookup instead of re-querying the user.
 */
public final class UserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;
    @Getter
    private final Role role;
    @Getter
    private final long tokenVersion;
    private final List<SimpleGrantedAuthority> authorities;

    public UserPrincipal(String username, String password, Role role, long tokenVersion) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.tokenVersion = tokenVersion;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getTokenVersion()
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
