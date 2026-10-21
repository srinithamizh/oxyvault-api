package in.srinithamizh.oxyvault.repository;

import in.srinithamizh.oxyvault.entity.Category;
import in.srinithamizh.oxyvault.enums.CategoryType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByUserIdOrderByNameAsc(UUID userId);

    List<Category> findAllByUserIdAndTypeOrderByNameAsc(UUID userId, CategoryType type);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Category c where c.id = :id and c.user.id = :userId")
    Optional<Category> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}
