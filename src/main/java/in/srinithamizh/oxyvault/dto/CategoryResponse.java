package in.srinithamizh.oxyvault.dto;

import in.srinithamizh.oxyvault.enums.CategoryType;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, UUID userId, CategoryType type) {
}
