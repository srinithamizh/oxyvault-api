package in.srinithamizh.oxyvault.repository;

import in.srinithamizh.oxyvault.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaction t where t.id = :id and t.user.id = :userId")
    Optional<Transaction> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsBySourceId(UUID sourceId);

    boolean existsByPaymentId(UUID paymentId);

    boolean existsByCategoryId(UUID categoryId);
}
