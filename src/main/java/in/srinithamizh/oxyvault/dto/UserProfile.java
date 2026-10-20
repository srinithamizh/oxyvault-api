package in.srinithamizh.oxyvault.dto;

import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.enums.Role;

import java.util.UUID;

public record UserProfile(UUID id, String name,  String username, String email, Role role) {
}
