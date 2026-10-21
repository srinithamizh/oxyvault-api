package in.srinithamizh.oxyvault.dto;

import in.srinithamizh.oxyvault.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String description,
        BigDecimal amount,
        UUID sourceId,
        UUID paymentId,
        UUID categoryId,
        Currency currency,
        String additionalInfo,
        Instant createdAt,
        Instant updatedAt
) {
}
