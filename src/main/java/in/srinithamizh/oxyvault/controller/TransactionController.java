package in.srinithamizh.oxyvault.controller;

import in.srinithamizh.oxyvault.dto.TransactionRequest;
import in.srinithamizh.oxyvault.dto.TransactionResponse;
import in.srinithamizh.oxyvault.security.UserPrincipal;
import in.srinithamizh.oxyvault.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public Page<TransactionResponse> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID sourceId,
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be before or equal to 'to'");
        }
        return transactionService.getTransactions(
                principal.getId(), sourceId, paymentId, categoryId, from, to, page, size
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionRequest request
    ) {
        return transactionService.createTransaction(principal.getId(), request);
    }

    @PutMapping("/{transactionId}")
    public TransactionResponse updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID transactionId,
            @Valid @RequestBody TransactionRequest request
    ) {
        return transactionService.updateTransaction(principal.getId(), transactionId, request);
    }

    @DeleteMapping("/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID transactionId
    ) {
        transactionService.deleteTransaction(principal.getId(), transactionId);
    }
}
