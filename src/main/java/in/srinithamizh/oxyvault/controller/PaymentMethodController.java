package in.srinithamizh.oxyvault.controller;

import in.srinithamizh.oxyvault.dto.PaymentMethodRequest;
import in.srinithamizh.oxyvault.dto.PaymentMethodResponse;
import in.srinithamizh.oxyvault.security.UserPrincipal;
import in.srinithamizh.oxyvault.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public List<PaymentMethodResponse> getPaymentMethods(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return paymentMethodService.getPaymentMethods(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse createPaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PaymentMethodRequest request
    ) {
        return paymentMethodService.createPaymentMethod(principal.getId(), request);
    }

    @PutMapping("/{paymentMethodId}")
    public PaymentMethodResponse updatePaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentMethodId,
            @Valid @RequestBody PaymentMethodRequest request
    ) {
        return paymentMethodService.updatePaymentMethod(principal.getId(), paymentMethodId, request);
    }

    @DeleteMapping("/{paymentMethodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePaymentMethod(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID paymentMethodId
    ) {
        paymentMethodService.deletePaymentMethod(principal.getId(), paymentMethodId);
    }
}
