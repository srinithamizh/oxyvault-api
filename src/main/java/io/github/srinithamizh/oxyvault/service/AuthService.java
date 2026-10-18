package io.github.srinithamizh.oxyvault.service;

import io.github.srinithamizh.oxyvault.dto.AuthRequest;
import io.github.srinithamizh.oxyvault.dto.AuthResponse;
import io.github.srinithamizh.oxyvault.dto.RegisterRequest;
import io.github.srinithamizh.oxyvault.entity.User;
import io.github.srinithamizh.oxyvault.enums.Role;
import io.github.srinithamizh.oxyvault.exception.UserAlreadyExistsException;
import io.github.srinithamizh.oxyvault.exception.UserNotFoundException;
import io.github.srinithamizh.oxyvault.jwt.JwtService;
import io.github.srinithamizh.oxyvault.repository.UserRepository;
import io.github.srinithamizh.oxyvault.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.username().trim();
        // Locale.ROOT: a plain toLowerCase() breaks under some default locales (e.g. Turkish "I").
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations can both pass the exists checks above;
            // the unique constraints are the real guard, so report it as a conflict, not a 500.
            log.debug("Registration rejected by a database constraint", e);
            throw new UserAlreadyExistsException("Username or email already registered");
        }
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier().trim(), request.password()));

        // The principal was already loaded during authentication, so no second user query is needed.
        // instanceof is false for null, so this also covers the null case.
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Unexpected principal after successful authentication");
        }

        return new AuthResponse(jwtService.generateToken(principal));
    }

    @Transactional
    public void logout(String username) {
        // Single atomic UPDATE: no read-modify-write, and concurrent logouts cannot lose an increment.
        if (userRepository.incrementTokenVersion(username) == 0) {
            throw new UserNotFoundException("User not found");
        }
    }
}
