package org.benefitmap.backend.catalog;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/search")
    public List<CatalogItemDto> search(@RequestBody CatalogSearchRequestDto request) {
        return catalogService.searchCatalog(request);
    }

    @GetMapping("/{id}")
    public CatalogItemDto getDetail(@PathVariable String id) {
        return catalogService.getCatalogItem(id);
    }
}
