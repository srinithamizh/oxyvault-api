package io.github.srinithamizh.oxyvault.entity;

import io.github.srinithamizh.oxyvault.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


@Table(name="users")
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, unique = true)
    private Long tokenVersion = 0L;
}
