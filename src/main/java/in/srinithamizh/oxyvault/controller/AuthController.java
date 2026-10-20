package in.srinithamizh.oxyvault.controller;

import in.srinithamizh.oxyvault.dto.AuthRequest;
import in.srinithamizh.oxyvault.dto.AuthResponse;
import in.srinithamizh.oxyvault.dto.RegisterRequest;
import in.srinithamizh.oxyvault.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/validate-username")
    public ResponseEntity<Boolean> isUsernameExists(@RequestParam String username) {
        return ResponseEntity.ok(authService.isUsernameExists(username));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        authService.logout(authentication.getName());

        return ResponseEntity.ok("User logged out");
    }
}
