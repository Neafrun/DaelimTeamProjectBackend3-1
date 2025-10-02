package org.benefitmap.backend.catalog;

import lombok.Data;
import java.util.List;

@Data
public class UserFilterDto {
    private Integer age;                    // 나이 (예: 30)
    private String ctpvNm;                  // 지역 - 시/도 (예: "경기도")
    private String sggNm;                   // 지역 - 시/군/구 (예: "수원시")
    private List<String> lifeStages;        // 생애주기 (예: ["청년"])
    private List<String> familyTypes;       // 가구상황 (예: ["한부모·조손", "다자녀"])
    private List<String> interests;         // 관심주제 (예: ["주거", "일자리"])
    private String keyword;                 // 키워드 검색
}
