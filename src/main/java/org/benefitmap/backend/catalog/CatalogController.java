package org.benefitmap.backend.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final WelfareFilterService filterService;
    private final ObjectMapper objectMapper;

    public CatalogController(CatalogService catalogService,
                             WelfareFilterService filterService) {
        this.catalogService = catalogService;
        this.filterService = filterService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 전체 복지 서비스 목록 (기존)
     */
    @GetMapping
    public Map<String, Object> getCatalog() {
        return catalogService.getCatalogList();
    }

    /**
     * 필터링된 복지 서비스 목록 (신규)
     */
    @PostMapping("/filter")
    public Map<String, Object> getFilteredCatalog(@RequestBody UserFilterDto filter) {
        try {
            // 1. 전체 복지 서비스 가져오기
            Map<String, Object> catalogData = catalogService.getCatalogList();

            // 2. items 배열 추출
            JsonNode body = (JsonNode) catalogData.get("body");
            JsonNode itemsNode = body.get("items");

            // 3. List<WelfareItemDto>로 변환
            List<WelfareItemDto> allServices = new ArrayList<>();
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    WelfareItemDto item = objectMapper.treeToValue(itemNode, WelfareItemDto.class);
                    allServices.add(item);
                }
            }

            // 4. 나이를 생애주기로 변환 (나이가 있고 생애주기가 없으면)
            if (filter.getAge() != null &&
                    (filter.getLifeStages() == null || filter.getLifeStages().isEmpty())) {
                String lifeStage = filterService.convertAgeToLifeStage(filter.getAge());
                filter.setLifeStages(Collections.singletonList(lifeStage));
            }

            // 5. 필터링 적용
            List<WelfareItemDto> filteredServices =
                    filterService.filterWelfareServices(allServices, filter);

            // 6. 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", filteredServices.size());
            result.put("items", filteredServices);
            result.put("filter", filter);  // 적용된 필터 정보도 함께 반환

            return result;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 특정 복지 서비스 상세 조회
     */
    @GetMapping("/{servId}")
    public WelfareItemDto getWelfareDetail(@PathVariable String servId) {
        try {
            Map<String, Object> catalogData = catalogService.getCatalogList();
            JsonNode body = (JsonNode) catalogData.get("body");
            JsonNode itemsNode = body.get("items");

            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    WelfareItemDto item = objectMapper.treeToValue(itemNode, WelfareItemDto.class);
                    if (item.getServId().equals(servId)) {
                        return item;
                    }
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 키워드 검색
     */
    @GetMapping("/search")
    public Map<String, Object> searchWelfare(@RequestParam String keyword) {
        UserFilterDto filter = new UserFilterDto();
        filter.setKeyword(keyword);
        return getFilteredCatalog(filter);
    }
}