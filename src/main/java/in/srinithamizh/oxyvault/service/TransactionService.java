package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.dto.TransactionRequest;
import in.srinithamizh.oxyvault.dto.TransactionResponse;
import in.srinithamizh.oxyvault.entity.Category;
import in.srinithamizh.oxyvault.entity.PaymentMethod;
import in.srinithamizh.oxyvault.entity.Source;
import in.srinithamizh.oxyvault.entity.Transaction;
import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.enums.CategoryType;
import in.srinithamizh.oxyvault.exception.CategoryNotFoundException;
import in.srinithamizh.oxyvault.exception.InsufficientSourceBalanceException;
import in.srinithamizh.oxyvault.exception.PaymentMethodNotFoundException;
import in.srinithamizh.oxyvault.exception.SourceNotFoundException;
import in.srinithamizh.oxyvault.exception.TransactionNotFoundException;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.CategoryRepository;
import in.srinithamizh.oxyvault.repository.PaymentMethodRepository;
import in.srinithamizh.oxyvault.repository.SourceRepository;
import in.srinithamizh.oxyvault.repository.TransactionRepository;
import in.srinithamizh.oxyvault.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SourceRepository sourceRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            UUID userId,
            UUID sourceId,
            UUID paymentId,
            UUID categoryId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Specification<Transaction> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            if (sourceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("source").get("id"), sourceId));
            }
            if (paymentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("payment").get("id"), paymentId));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return transactionRepository.findAll(specification, pageable)
                .map(TransactionService::toResponse);
    }

    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Source source = lockOwnedSources(userId, List.of(request.sourceId())).get(request.sourceId());
        PaymentMethod payment = findOwnedPaymentMethod(request.paymentId(), userId);
        Category category = findOwnedCategory(request.categoryId(), userId);
        applyBalanceChange(source, transactionBalanceEffect(request.amount(), category.getType()));

        Transaction transaction = Transaction.builder()
                .user(user)
                .description(request.description().trim())
                .amount(request.amount())
                .source(source)
                .payment(payment)
                .category(category)
                .currency(request.currency())
                .additionalInfo(request.additionalInfo())
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created transaction id={} for user id={}", savedTransaction.getId(), userId);
        return toResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(UUID userId, UUID transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserIdForUpdate(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        Source oldSource = transaction.getSource();
        Category oldCategory = transaction.getCategory();
        Map<UUID, Source> sources = lockOwnedSources(userId, List.of(oldSource.getId(), request.sourceId()));
        Source newSource = sources.get(request.sourceId());
        PaymentMethod payment = findOwnedPaymentMethod(request.paymentId(), userId);
        Category category = findOwnedCategory(request.categoryId(), userId);

        BigDecimal oldBalanceEffect = transactionBalanceEffect(transaction.getAmount(), oldCategory.getType());
        BigDecimal newBalanceEffect = transactionBalanceEffect(request.amount(), category.getType());
        if (oldSource.getId().equals(newSource.getId())) {
            applyBalanceChange(newSource, newBalanceEffect.subtract(oldBalanceEffect));
        } else {
            applyBalanceChange(oldSource, oldBalanceEffect.negate());
            applyBalanceChange(newSource, newBalanceEffect);
        }

        transaction.setDescription(request.description().trim());
        transaction.setAmount(request.amount());
        transaction.setSource(newSource);
        transaction.setPayment(payment);
        transaction.setCategory(category);
        transaction.setCurrency(request.currency());
        transaction.setAdditionalInfo(request.additionalInfo());
        log.info("Updated transaction id={} for user id={}", transactionId, userId);
        return toResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserIdForUpdate(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        Source source = lockOwnedSources(userId, List.of(transaction.getSource().getId()))
                .get(transaction.getSource().getId());
        BigDecimal balanceEffect = transactionBalanceEffect(transaction.getAmount(), transaction.getCategory().getType());
        applyBalanceChange(source, balanceEffect.negate());
        transactionRepository.delete(transaction);
        log.info("Deleted transaction id={} for user id={}", transactionId, userId);
    }

    private Map<UUID, Source> lockOwnedSources(UUID userId, List<UUID> sourceIds) {
        Map<UUID, Source> sources = new HashMap<>();
        sourceIds.stream().distinct().sorted(Comparator.naturalOrder()).forEach(sourceId ->
                sources.put(
                        sourceId,
                        sourceRepository.findByIdAndUserIdForUpdate(sourceId, userId)
                                .orElseThrow(() -> new SourceNotFoundException("Source not found"))
                )
        );
        return sources;
    }

    private PaymentMethod findOwnedPaymentMethod(UUID paymentId, UUID userId) {
        return paymentMethodRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));
    }

    private Category findOwnedCategory(UUID categoryId, UUID userId) {
        return categoryRepository.findByIdAndUserIdForUpdate(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }

    private void applyBalanceChange(Source source, BigDecimal change) {
        BigDecimal updatedBalance = source.getBalance().add(change);
        if (updatedBalance.signum() < 0) {
            throw new InsufficientSourceBalanceException(
                    "Insufficient balance in the selected source to record this transaction."
            );
        }
        source.setBalance(updatedBalance);
        sourceRepository.saveAndFlush(source);
    }

    private static BigDecimal transactionBalanceEffect(BigDecimal amount, CategoryType categoryType) {
        return categoryType == CategoryType.EXPENSE ? amount.negate() : amount;
    }

    private static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getSource().getId(),
                transaction.getPayment().getId(),
                transaction.getCategory().getId(),
                transaction.getCurrency(),
                transaction.getAdditionalInfo(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
