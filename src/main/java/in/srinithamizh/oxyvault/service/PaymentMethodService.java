package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.dto.PaymentMethodRequest;
import in.srinithamizh.oxyvault.dto.PaymentMethodResponse;
import in.srinithamizh.oxyvault.entity.PaymentMethod;
import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.exception.PaymentMethodNotFoundException;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.PaymentMethodRepository;
import in.srinithamizh.oxyvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getPaymentMethods(UUID userId) {
        return paymentMethodRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(PaymentMethodService::toResponse)
                .toList();
    }

    @Transactional
    public PaymentMethodResponse createPaymentMethod(UUID userId, PaymentMethodRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .name(request.name().trim())
                .user(user)
                .build();
        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Created payment method id={} for user id={}", savedPaymentMethod.getId(), userId);
        return toResponse(savedPaymentMethod);
    }

    @Transactional
    public PaymentMethodResponse updatePaymentMethod(
            UUID userId,
            UUID paymentMethodId,
            PaymentMethodRequest request
    ) {
        PaymentMethod paymentMethod = findOwnedPaymentMethod(userId, paymentMethodId);
        paymentMethod.setName(request.name().trim());
        log.info("Updated payment method id={} for user id={}", paymentMethodId, userId);
        return toResponse(paymentMethod);
    }

    @Transactional
    public void deletePaymentMethod(UUID userId, UUID paymentMethodId) {
        paymentMethodRepository.delete(findOwnedPaymentMethod(userId, paymentMethodId));
        log.info("Deleted payment method id={} for user id={}", paymentMethodId, userId);
    }

    private PaymentMethod findOwnedPaymentMethod(UUID userId, UUID paymentMethodId) {
        return paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));
    }

    private static PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(paymentMethod.getId(), paymentMethod.getName());
    }
}
