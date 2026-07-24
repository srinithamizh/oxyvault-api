package io.github.srinithamizh.oxyvault.dto;


public record RegisterRequest(
        String username,
        String email,
        String password

) {
}