package in.srinithamizh.oxyvault.dto;

import in.srinithamizh.oxyvault.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotNull Role role) {
}
