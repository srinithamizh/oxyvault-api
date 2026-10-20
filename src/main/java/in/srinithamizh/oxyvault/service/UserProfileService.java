package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.dto.UserProfile;
import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.UserRepository;
import in.srinithamizh.oxyvault.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    private UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfile getUserProfile(String username) {
        User currentUser =  userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return new UserProfile(currentUser.getId(), currentUser.getFirstName() +" "+currentUser.getLastName(), username, currentUser.getEmail(), currentUser.getRole());
    }
}
