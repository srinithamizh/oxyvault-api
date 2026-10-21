package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.dto.CategoryRequest;
import in.srinithamizh.oxyvault.dto.CategoryResponse;
import in.srinithamizh.oxyvault.entity.Category;
import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.enums.CategoryType;
import in.srinithamizh.oxyvault.exception.CategoryNotFoundException;
import in.srinithamizh.oxyvault.exception.TransactionReferenceConflictException;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.CategoryRepository;
import in.srinithamizh.oxyvault.repository.TransactionRepository;
import in.srinithamizh.oxyvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID userId, CategoryType type) {
        List<Category> categories = type == null
                ? categoryRepository.findAllByUserIdOrderByNameAsc(userId)
                : categoryRepository.findAllByUserIdAndTypeOrderByNameAsc(userId, type);
        return categories.stream()
                .map(CategoryService::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(UUID userId, CategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Category category = Category.builder()
                .name(request.name().trim())
                .user(user)
                .type(request.type())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(UUID userId, UUID categoryId, CategoryRequest request) {
        Category category = findOwnedCategory(userId, categoryId);
        if (category.getType() != request.type() && transactionRepository.existsByCategoryId(categoryId)) {
            throw new TransactionReferenceConflictException(
                    "You can't change this category's type because it is recorded in a transaction."
            );
        }
        category.setName(request.name().trim());
        category.setType(request.type());
        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        Category category = findOwnedCategory(userId, categoryId);
        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw new TransactionReferenceConflictException(
                    "You can't delete this category because it is recorded in a transaction. "
                            + "Update the transaction and then delete this category."
            );
        }
        categoryRepository.delete(category);
    }

    private Category findOwnedCategory(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserIdForUpdate(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }

    private static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getUser().getId(),
                category.getType()
        );
    }
}
