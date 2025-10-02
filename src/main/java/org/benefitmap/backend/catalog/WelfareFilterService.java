package org.benefitmap.backend.catalog;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WelfareFilterService {

    /**
     * 사용자 필터 조건에 맞는 복지 서비스만 반환
     */
    public List<WelfareItemDto> filterWelfareServices(
            List<WelfareItemDto> allServices,
            UserFilterDto filter) {

        return allServices.stream()
                .filter(service -> matchesFilter(service, filter))
                .collect(Collectors.toList());
    }

    /**
     * 개별 복지 서비스가 사용자 조건에 맞는지 체크
     */
    private boolean matchesFilter(WelfareItemDto service, UserFilterDto filter) {

        // 1. 지역 필터 (시/도)
        if (filter.getCtpvNm() != null && !filter.getCtpvNm().isEmpty()) {
            // 전국 단위 서비스는 무조건 포함
            if (service.getCtpvNm() != null && !service.getCtpvNm().isEmpty()) {
                if (!service.getCtpvNm().contains(filter.getCtpvNm())) {
                    return false;
                }
            }
        }

        // 2. 지역 필터 (시/군/구)
        if (filter.getSggNm() != null && !filter.getSggNm().isEmpty()) {
            if (service.getSggNm() != null && !service.getSggNm().isEmpty()) {
                if (!service.getSggNm().contains(filter.getSggNm())) {
                    return false;
                }
            }
        }

        // 3. 생애주기 필터
        if (filter.getLifeStages() != null && !filter.getLifeStages().isEmpty()) {
            if (!matchesLifeStage(service, filter)) {
                return false;
            }
        }

        // 4. 가구상황 필터
        if (filter.getFamilyTypes() != null && !filter.getFamilyTypes().isEmpty()) {
            if (!matchesFamilyType(service, filter)) {
                return false;
            }
        }

        // 5. 관심주제 필터
        if (filter.getInterests() != null && !filter.getInterests().isEmpty()) {
            if (!matchesInterest(service, filter)) {
                return false;
            }
        }

        // 6. 키워드 검색
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            if (!matchesKeyword(service, filter.getKeyword())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 생애주기 매칭
     */
    private boolean matchesLifeStage(WelfareItemDto service, UserFilterDto filter) {
        if (service.getLifeNmArray() == null || service.getLifeNmArray().isEmpty()) {
            return true; // 생애주기 제한 없음
        }

        String lifeStages = service.getLifeNmArray();

        for (String userLifeStage : filter.getLifeStages()) {
            if (lifeStages.contains(userLifeStage)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 가구상황 매칭
     */
    private boolean matchesFamilyType(WelfareItemDto service, UserFilterDto filter) {
        if (service.getTrgterIndvdlNmArray() == null || service.getTrgterIndvdlNmArray().isEmpty()) {
            return true; // 가구상황 제한 없음
        }

        String familyTypes = service.getTrgterIndvdlNmArray();

        for (String userFamilyType : filter.getFamilyTypes()) {
            if (familyTypes.contains(userFamilyType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 관심주제 매칭
     */
    private boolean matchesInterest(WelfareItemDto service, UserFilterDto filter) {
        if (service.getIntrsThemaNmArray() == null || service.getIntrsThemaNmArray().isEmpty()) {
            return true; // 관심주제 제한 없음
        }

        String interests = service.getIntrsThemaNmArray();

        for (String userInterest : filter.getInterests()) {
            if (interests.contains(userInterest)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 키워드 검색 (서비스명, 서비스 요약에서 검색)
     */
    private boolean matchesKeyword(WelfareItemDto service, String keyword) {
        String lowerKeyword = keyword.toLowerCase();

        // 서비스명에서 검색
        if (service.getServNm() != null &&
                service.getServNm().toLowerCase().contains(lowerKeyword)) {
            return true;
        }

        // 서비스 요약에서 검색
        if (service.getServDgst() != null &&
                service.getServDgst().toLowerCase().contains(lowerKeyword)) {
            return true;
        }

        return false;
    }

    /**
     * 나이를 생애주기로 변환
     */
    public String convertAgeToLifeStage(int age) {
        if (age < 6) return "영유아";
        if (age < 13) return "아동";
        if (age < 20) return "청소년";
        if (age < 35) return "청년";
        if (age < 65) return "중장년";
        return "노년";
    }
}
