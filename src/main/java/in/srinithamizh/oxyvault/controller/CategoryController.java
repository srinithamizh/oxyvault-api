package in.srinithamizh.oxyvault.controller;

import in.srinithamizh.oxyvault.dto.CategoryRequest;
import in.srinithamizh.oxyvault.dto.CategoryResponse;
import in.srinithamizh.oxyvault.security.UserPrincipal;
import in.srinithamizh.oxyvault.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> getCategories(@AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.getCategories(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CategoryRequest request
    ) {
        return categoryService.createCategory(principal.getId(), request);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse updateCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequest request
    ) {
        return categoryService.updateCategory(principal.getId(), categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID categoryId
    ) {
        categoryService.deleteCategory(principal.getId(), categoryId);
    }
}
