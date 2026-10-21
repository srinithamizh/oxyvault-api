package in.srinithamizh.oxyvault.service;

import in.srinithamizh.oxyvault.dto.CategoryRequest;
import in.srinithamizh.oxyvault.dto.CategoryResponse;
import in.srinithamizh.oxyvault.entity.Category;
import in.srinithamizh.oxyvault.entity.User;
import in.srinithamizh.oxyvault.exception.CategoryNotFoundException;
import in.srinithamizh.oxyvault.exception.UserNotFoundException;
import in.srinithamizh.oxyvault.repository.CategoryRepository;
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
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID userId) {
        return categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
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
        category.setName(request.name().trim());
        category.setType(request.type());
        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(UUID userId, UUID categoryId) {
        categoryRepository.delete(findOwnedCategory(userId, categoryId));
    }

    private Category findOwnedCategory(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
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
