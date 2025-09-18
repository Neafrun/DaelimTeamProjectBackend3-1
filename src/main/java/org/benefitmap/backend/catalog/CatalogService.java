package org.benefitmap.backend.catalog;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class CatalogService {

    private final WebClient webClient;

    public CatalogService(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<CatalogItemDto> searchCatalog(CatalogSearchRequestDto request) {
        // TODO: 복지로 Open API 호출 후 데이터 매핑
        return List.of(
                new CatalogItemDto("1", "예시 복지1", "설명1", "중앙정부"),
                new CatalogItemDto("2", "예시 복지2", "설명2", "지자체"));
    }

    public CatalogItemDto getCatalogItem(String id) {
        // TODO: 상세 조회 구현
        return new CatalogItemDto(id, "예시 복지 상세", "상세 설명", "중앙정부");
    }
}
