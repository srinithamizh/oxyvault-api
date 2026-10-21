package in.srinithamizh.oxyvault.repository;

import in.srinithamizh.oxyvault.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<PaymentMethod> findByIdAndUserId(UUID id, UUID userId);
}
