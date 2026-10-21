package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.dto.UserProfile;
import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.enums.Role;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    public Page<UserProfile> getAllUsers(Pageable pageable) {
        Pageable boundedPageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                pageable.getSort()
        );
        log.debug("Listing users page={} size={}", boundedPageable.getPageNumber(), boundedPageable.getPageSize());
        return userRepository.findAllUserProfiles(boundedPageable);
    }

    @Transactional
    public UserProfile updateUserRole(UUID userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Role previousRole = user.getRole();
        user.setRole(role);
        log.info("Updated role for user id={} from {} to {}", userId, previousRole, role);

        return new UserProfile(
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
