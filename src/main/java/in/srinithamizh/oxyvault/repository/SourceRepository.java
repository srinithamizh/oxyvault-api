package in.srinithamizh.oxyvault.repository;

import in.srinithamizh.oxyvault.entity.Source;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceRepository extends JpaRepository<Source, UUID> {

    List<Source> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<Source> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Source s where s.id = :id and s.user.id = :userId")
    Optional<Source> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
