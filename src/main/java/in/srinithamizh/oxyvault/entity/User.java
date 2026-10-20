package in.srinithamizh.oxyvault.entity;

import in.srinithamizh.oxyvault.enums.Role;
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

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    // Not unique: every user starts at 0 and it is bumped on logout.
    // @Builder.Default is required, otherwise the builder ignores "= 0L" and inserts null.
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Long tokenVersion = 0L;
}
