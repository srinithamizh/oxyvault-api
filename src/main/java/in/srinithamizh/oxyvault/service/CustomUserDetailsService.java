package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.UserRepository;
import in.srinithamizh.oxyvault.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Used by the AuthenticationManager at login. The identifier is a username or an email.
     * Exactly one query runs, and the lookup is deterministic: it never matches two different users.
     */
    @Override
    public UserPrincipal loadUserByUsername(String identifier) {
        return findByIdentifier(identifier.trim())
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /**
     * Used by the JWT filter. The token subject is always the exact username.
     */
    public UserPrincipal loadByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private Optional<User> findByIdentifier(String identifier) {
        if (identifier.indexOf('@') >= 0) {
            // Emails are stored lower-cased. Fall back to username for legacy accounts containing '@'.
            return userRepository.findByEmail(identifier.toLowerCase(Locale.ROOT))
                    .or(() -> userRepository.findByUsername(identifier));
        }
        return userRepository.findByUsername(identifier);
    }
}
