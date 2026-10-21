package in.srinithamizh.oxyvault.entity;

import in.srinithamizh.oxyvault.enums.AccountType;
import in.srinithamizh.oxyvault.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sources", indexes = @Index(name = "idx_sources_user_id", columnList = "user_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType type;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "credit_limit", precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(length = 100)
    private String provider;

    @Column(precision = 19, scale = 2)
    private BigDecimal emi;

    @Column(name = "interest_rate", precision = 7, scale = 4)
    private BigDecimal interestRate;
}
