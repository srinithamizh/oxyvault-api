package in.srinithamizh.oxyvault.controller;

import in.srinithamizh.oxyvault.dto.UserProfile;
import in.srinithamizh.oxyvault.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getCurrentUser(Authentication authentication) {

        return ResponseEntity.ok(userProfileService.getUserProfile(authentication.getName()));
    }
}
