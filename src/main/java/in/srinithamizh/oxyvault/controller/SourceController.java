package in.srinithamizh.oxyvault.controller;

import in.srinithamizh.oxyvault.dto.SourceRequest;
import in.srinithamizh.oxyvault.dto.SourceResponse;
import in.srinithamizh.oxyvault.security.UserPrincipal;
import in.srinithamizh.oxyvault.service.SourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceService sourceService;

    @GetMapping
    public List<SourceResponse> getSources(@AuthenticationPrincipal UserPrincipal principal) {
        return sourceService.getSources(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SourceResponse createSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SourceRequest request
    ) {
        return sourceService.createSource(principal.getId(), request);
    }

    @PutMapping("/{sourceId}")
    public SourceResponse updateSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sourceId,
            @Valid @RequestBody SourceRequest request
    ) {
        return sourceService.updateSource(principal.getId(), sourceId, request);
    }

    @DeleteMapping("/{sourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sourceId
    ) {
        sourceService.deleteSource(principal.getId(), sourceId);
    }
}
