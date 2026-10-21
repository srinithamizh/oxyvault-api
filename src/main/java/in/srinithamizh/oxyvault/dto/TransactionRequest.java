package in.srinithamizh.oxyvault.dto;

import in.srinithamizh.oxyvault.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Amount must have at most two decimal places")
        BigDecimal amount,

        @NotNull(message = "Source is required")
        UUID sourceId,

        @NotNull(message = "Payment is required")
        UUID paymentId,

        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotNull(message = "Currency is required")
        Currency currency,

        @Size(max = 2000, message = "Additional description must be at most 2000 characters")
        String additionalInfo
) {
}
