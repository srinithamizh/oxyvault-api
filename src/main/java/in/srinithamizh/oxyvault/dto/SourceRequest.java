package in.srinithamizh.oxyvault.dto;

import in.srinithamizh.oxyvault.enums.AccountType;
import in.srinithamizh.oxyvault.enums.SourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SourceRequest(
        @NotNull(message = "Type is required")
        SourceType type,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        AccountType accountType,

        @Pattern(regexp = "\\d{4}", message = "Last four must contain exactly four digits")
        String lastFour,

        @NotNull(message = "Balance is required")
        @DecimalMin(value = "0.00", message = "Balance cannot be negative")
        BigDecimal balance,

        @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
        BigDecimal creditLimit,

        @Size(max = 100, message = "Provider must be at most 100 characters")
        String provider,

        @DecimalMin(value = "0.00", message = "EMI cannot be negative")
        BigDecimal emi,

        @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
        BigDecimal interestRate
) {
}
