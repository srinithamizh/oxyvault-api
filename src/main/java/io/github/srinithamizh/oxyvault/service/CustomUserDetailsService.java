package io.github.srinithamizh.oxyvault.service;

import io.github.srinithamizh.oxyvault.exception.UserNotFoundException;
import io.github.srinithamizh.oxyvault.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        var user = userRepository.findByUsernameOrEmail(identifier, identifier).orElseThrow(() -> new UserNotFoundException("User not found with username/email: " + identifier));

        return User.withUsername(user.getUsername()).password(user.getPassword()).roles(user.getRole().name()).build();

    }
}
