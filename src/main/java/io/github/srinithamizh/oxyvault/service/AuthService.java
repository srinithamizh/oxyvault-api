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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public void register(RegisterRequest request) {
        String username = request.username()
                .trim();

        String email = request.email()
                .trim()
                .toLowerCase();


        // Check duplicate username
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(
                    "Username already exists"
            );
        }


        // Check duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "Email already registered"
            );
        }


        User user = User.builder()
                .username(username)
                .email(email)
                .password(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .role(Role.USER)
                .build();


        userRepository.save(user);
    }

    public AuthResponse login(AuthRequest request) {
        String identifier = request.identifier().trim();

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, request.password()));

        User user = userRepository.findByUsernameOrEmail(identifier,identifier)
                .orElseThrow(() -> new UserNotFoundException("Invalid credentials"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(token);
    }

    @Transactional
    public void logout(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        user.setTokenVersion(user.getTokenVersion()+1);

        userRepository.save(user);
    }
}
