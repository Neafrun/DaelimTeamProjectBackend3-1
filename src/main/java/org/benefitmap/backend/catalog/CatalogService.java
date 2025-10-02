package org.benefitmap.backend.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class CatalogService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${catalog.api.url}")
    private String apiUrl;

    @Value("${catalog.api.service-key}")
    private String serviceKey;

    // Mock 모드 설정 (true = Mock 데이터 사용, false = 실제 API 사용)
    private final boolean USE_MOCK_DATA = true;  // ⭐ 여기만 true/false 변경!

    public CatalogService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Map<String, Object> getCatalogList() {
        // Mock 모드면 샘플 데이터 반환
        if (USE_MOCK_DATA) {
            return getMockCatalogData();
        }

        // 실제 API 호출
        return getRealCatalogData();
    }

    // 실제 API 호출 메서드
    private Map<String, Object> getRealCatalogData() {
        try {
            // API 호출
            String response = webClient.get()
                    .uri(apiUrl + "?serviceKey=" + serviceKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // JSON 파싱
            JsonNode root = objectMapper.readTree(response);

            Map<String, Object> result = new HashMap<>();
            JsonNode header = root.path("response").path("header");
            JsonNode body = root.path("response").path("body");

            result.put("header", header);
            result.put("body", body);

            return result;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    // Mock 데이터 반환 메서드
    private Map<String, Object> getMockCatalogData() {
        try {
            String mockJson = """
            {
                "response": {
                    "header": {
                        "resultCode": "00",
                        "resultMsg": "SUCCESS (MOCK DATA)"
                    },
                    "body": {
                        "totalCount": 150,
                        "pageNo": 1,
                        "numOfRows": 10,
                        "items": [
                            {
                                "servId": "MOCK001",
                                "servNm": "청년 월세 지원사업",
                                "bizChrDeptNm": "서울특별시 청년정책과",
                                "ctpvNm": "서울특별시",
                                "sggNm": "종로구",
                                "servDgst": "만 19세~34세 청년의 월세 부담을 줄이기 위한 지원 사업입니다. 월 최대 20만원까지 지원합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/001",
                                "lifeNmArray": "청년",
                                "trgterIndvdlNmArray": null,
                                "intrsThemaNmArray": "주거, 서민금융",
                                "sprtCycNm": "월",
                                "srvPvsnNm": "현금지급",
                                "aplyMtdNm": "온라인신청",
                                "inqNum": "15420",
                                "lastModYmd": "20250918"
                            },
                            {
                                "servId": "MOCK002",
                                "servNm": "다자녀 가정 교육비 지원",
                                "bizChrDeptNm": "경기도 가족정책과",
                                "ctpvNm": "경기도",
                                "sggNm": "수원시",
                                "servDgst": "3자녀 이상 다자녀 가정의 교육비를 지원하여 양육 부담을 경감하고 출산 장려 분위기를 조성합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/002",
                                "lifeNmArray": "아동, 청소년",
                                "trgterIndvdlNmArray": "다자녀",
                                "intrsThemaNmArray": "교육, 서민금융",
                                "sprtCycNm": "년",
                                "srvPvsnNm": "현금지급",
                                "aplyMtdNm": "방문신청",
                                "inqNum": "8923",
                                "lastModYmd": "20250915"
                            },
                            {
                                "servId": "MOCK003",
                                "servNm": "저소득층 의료비 지원",
                                "bizChrDeptNm": "부산광역시 보건복지과",
                                "ctpvNm": "부산광역시",
                                "sggNm": "해운대구",
                                "servDgst": "기초생활수급자 및 차상위계층의 의료비 부담을 줄이기 위한 지원 사업입니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/003",
                                "lifeNmArray": "영유아, 아동, 청소년, 청년, 중장년, 노년",
                                "trgterIndvdlNmArray": "저소득",
                                "intrsThemaNmArray": "신체건강, 서민금융",
                                "sprtCycNm": "수시",
                                "srvPvsnNm": "현금지급",
                                "aplyMtdNm": "방문신청",
                                "inqNum": "12456",
                                "lastModYmd": "20250910"
                            },
                            {
                                "servId": "MOCK004",
                                "servNm": "노인 일자리 지원사업",
                                "bizChrDeptNm": "인천광역시 노인복지과",
                                "ctpvNm": "인천광역시",
                                "sggNm": "남동구",
                                "servDgst": "만 65세 이상 어르신들에게 일자리를 제공하여 소득 창출 및 사회참여 기회를 제공합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/004",
                                "lifeNmArray": "노년",
                                "trgterIndvdlNmArray": null,
                                "intrsThemaNmArray": "일자리, 서민금융",
                                "sprtCycNm": "월",
                                "srvPvsnNm": "현금지급",
                                "aplyMtdNm": "방문신청, 전화신청",
                                "inqNum": "9832",
                                "lastModYmd": "20250912"
                            },
                            {
                                "servId": "MOCK005",
                                "servNm": "한부모 가정 양육비 지원",
                                "bizChrDeptNm": "대전광역시 여성가족과",
                                "ctpvNm": "대전광역시",
                                "sggNm": "서구",
                                "servDgst": "한부모 및 조손가정의 양육비를 지원하여 경제적 부담을 완화하고 아동의 건강한 성장을 돕습니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/005",
                                "lifeNmArray": "아동, 청소년",
                                "trgterIndvdlNmArray": "한부모·조손",
                                "intrsThemaNmArray": "서민금융",
                                "sprtCycNm": "월",
                                "srvPvsnNm": "현금지급",
                                "aplyMtdNm": "온라인신청, 방문신청",
                                "inqNum": "7621",
                                "lastModYmd": "20250916"
                            },
                            {
                                "servId": "MOCK006",
                                "servNm": "장애인 활동지원 서비스",
                                "bizChrDeptNm": "광주광역시 장애인복지과",
                                "ctpvNm": "광주광역시",
                                "sggNm": "북구",
                                "servDgst": "일상생활이 어려운 장애인에게 활동보조 서비스를 제공하여 자립생활을 지원합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/006",
                                "lifeNmArray": "청년, 중장년, 노년",
                                "trgterIndvdlNmArray": "장애인",
                                "intrsThemaNmArray": "신체건강, 일상생활지원",
                                "sprtCycNm": "월",
                                "srvPvsnNm": "서비스제공",
                                "aplyMtdNm": "방문신청",
                                "inqNum": "11234",
                                "lastModYmd": "20250914"
                            },
                            {
                                "servId": "MOCK007",
                                "servNm": "신혼부부 전세자금 대출",
                                "bizChrDeptNm": "세종특별자치시 주거복지과",
                                "ctpvNm": "세종특별자치시",
                                "sggNm": null,
                                "servDgst": "결혼 7년 이내 신혼부부에게 전세자금을 저금리로 대출하여 주거 안정을 지원합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/007",
                                "lifeNmArray": "청년, 중장년",
                                "trgterIndvdlNmArray": null,
                                "intrsThemaNmArray": "주거, 서민금융",
                                "sprtCycNm": "1회성",
                                "srvPvsnNm": "현금대여(융자)",
                                "aplyMtdNm": "온라인신청",
                                "inqNum": "18945",
                                "lastModYmd": "20250917"
                            },
                            {
                                "servId": "MOCK008",
                                "servNm": "임산부 건강관리 지원",
                                "bizChrDeptNm": "울산광역시 보건정책과",
                                "ctpvNm": "울산광역시",
                                "sggNm": "남구",
                                "servDgst": "임산부의 건강한 출산을 위한 산전 검사비 및 영양제를 지원합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/008",
                                "lifeNmArray": "임신·출산",
                                "trgterIndvdlNmArray": null,
                                "intrsThemaNmArray": "임신·출산, 신체건강",
                                "sprtCycNm": "1회성",
                                "srvPvsnNm": "현물지급",
                                "aplyMtdNm": "방문신청",
                                "inqNum": "6789",
                                "lastModYmd": "20250911"
                            },
                            {
                                "servId": "MOCK009",
                                "servNm": "청소년 문화활동 바우처",
                                "bizChrDeptNm": "강원도 청소년과",
                                "ctpvNm": "강원도",
                                "sggNm": "춘천시",
                                "servDgst": "청소년의 문화생활 향유 기회를 확대하기 위한 문화활동 바우처를 지원합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/009",
                                "lifeNmArray": "청소년",
                                "trgterIndvdlNmArray": null,
                                "intrsThemaNmArray": "문화·여가",
                                "sprtCycNm": "년",
                                "srvPvsnNm": "전자바우처(바우처)",
                                "aplyMtdNm": "온라인신청",
                                "inqNum": "5432",
                                "lastModYmd": "20250913"
                            },
                            {
                                "servId": "MOCK010",
                                "servNm": "농촌 귀농인 정착 지원",
                                "bizChrDeptNm": "충청북도 농업정책과",
                                "ctpvNm": "충청북도",
                                "sggNm": "청주시",
                                "servDgst": "귀농·귀촌인의 안정적인 정착을 위한 주택 수리비 및 농기계 구입비를 지원합니다.",
                                "servDtlLink": "https://www.bokjiro.go.kr/mock/010",
                                "lifeNmArray": "청년, 중장년",
                                "trgterIndvdlNmArray": null,
                                "intrsThemaNmArray": "일자리, 주거",
                                "sprtCycNm": "1회성",
                                "srvPvsnNm": "현금지급",
                                "aplyMtdNm": "방문신청",
                                "inqNum": "4156",
                                "lastModYmd": "20250908"
                            }
                        ]
                    }
                }
            }
            """;

            JsonNode root = objectMapper.readTree(mockJson);

            Map<String, Object> result = new HashMap<>();
            JsonNode header = root.path("response").path("header");
            JsonNode body = root.path("response").path("body");

            result.put("header", header);
            result.put("body", body);

            System.out.println("⚠️ MOCK 데이터 사용 중 - 공공데이터포털 정상화 후 USE_MOCK_DATA를 false로 변경하세요!");

            return result;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Mock 데이터 로드 실패: " + e.getMessage());
            return error;
        }
    }
}