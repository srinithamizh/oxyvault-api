package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.dto.SourceRequest;
import in.srinithamizh.oxyvault.dto.SourceResponse;
import in.srinithamizh.oxyvault.entity.Source;
import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.exception.SourceNotFoundException;
import in.srinithamizh.oxyvault.exception.TransactionReferenceConflictException;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.SourceRepository;
import in.srinithamizh.oxyvault.repository.TransactionRepository;
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
public class SourceService {

    private final SourceRepository sourceRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SourceResponse> getSources(UUID userId) {
        return sourceRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(SourceService::toResponse)
                .toList();
    }

    @Transactional
    public SourceResponse createSource(UUID userId, SourceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Source source = toSource(request, user);
        Source savedSource = sourceRepository.save(source);
        log.info("Created source id={} type={} for user id={}", savedSource.getId(), savedSource.getType(), userId);
        return toResponse(savedSource);
    }

    @Transactional
    public SourceResponse updateSource(UUID userId, UUID sourceId, SourceRequest request) {
        Source source = findOwnedSource(userId, sourceId);
        applyRequest(source, request);
        log.info("Updated source id={} type={} for user id={}", sourceId, source.getType(), userId);
        return toResponse(source);
    }

    @Transactional
    public void deleteSource(UUID userId, UUID sourceId) {
        Source source = findOwnedSource(userId, sourceId);
        if (transactionRepository.existsBySourceId(sourceId)) {
            throw new TransactionReferenceConflictException(
                    "You can't delete this source because it is recorded in a transaction. "
                            + "Update the transaction and then delete this source."
            );
        }
        sourceRepository.delete(source);
        log.info("Deleted source id={} for user id={}", sourceId, userId);
    }

    private Source findOwnedSource(UUID userId, UUID sourceId) {
        return sourceRepository.findByIdAndUserIdForUpdate(sourceId, userId)
                .orElseThrow(() -> new SourceNotFoundException("Source not found"));
    }

    private static Source toSource(SourceRequest request, User user) {
        Source source = Source.builder()
                .user(user)
                .build();
        applyRequest(source, request);
        return source;
    }

    private static void applyRequest(Source source, SourceRequest request) {
        source.setType(request.type());
        source.setName(request.name().trim());
        source.setAccountType(request.accountType());
        source.setLastFour(request.lastFour());
        source.setBalance(request.balance());
        source.setCreditLimit(request.creditLimit());
        source.setProvider(request.provider() == null ? null : request.provider().trim());
        source.setEmi(request.emi());
        source.setInterestRate(request.interestRate());
    }

    private static SourceResponse toResponse(Source source) {
        return new SourceResponse(
                source.getId(),
                source.getType(),
                source.getName(),
                source.getUser().getId(),
                source.getAccountType(),
                source.getLastFour(),
                source.getBalance(),
                source.getCreditLimit(),
                source.getProvider(),
                source.getEmi(),
                source.getInterestRate()
        );
    }
}
