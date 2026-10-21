package in.srinithamizh.oxyvault.dto;

import in.srinithamizh.oxyvault.enums.AccountType;
import in.srinithamizh.oxyvault.enums.SourceType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SourceResponse(
        UUID id,
        SourceType type,
        String name,
        UUID userId,
        AccountType accountType,
        String lastFour,
        BigDecimal balance,
        BigDecimal creditLimit,
        String provider,
        BigDecimal emi,
        BigDecimal interestRate
) {
}
