package org.benefitmap.backend.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WelfareItemDto {
    @JsonProperty("servId")
    private String servId;                  // 서비스 ID

    @JsonProperty("servNm")
    private String servNm;                  // 서비스명

    @JsonProperty("bizChrDeptNm")
    private String bizChrDeptNm;            // 담당부서

    @JsonProperty("ctpvNm")
    private String ctpvNm;                  // 시/도

    @JsonProperty("sggNm")
    private String sggNm;                   // 시/군/구

    @JsonProperty("servDgst")
    private String servDgst;                // 서비스 요약

    @JsonProperty("servDtlLink")
    private String servDtlLink;             // 상세링크

    @JsonProperty("lifeNmArray")
    private String lifeNmArray;             // 생애주기

    @JsonProperty("trgterIndvdlNmArray")
    private String trgterIndvdlNmArray;     // 대상자 (가구상황)

    @JsonProperty("intrsThemaNmArray")
    private String intrsThemaNmArray;       // 관심주제

    @JsonProperty("sprtCycNm")
    private String sprtCycNm;               // 지원주기

    @JsonProperty("srvPvsnNm")
    private String srvPvsnNm;               // 지원형태

    @JsonProperty("aplyMtdNm")
    private String aplyMtdNm;               // 신청방법

    @JsonProperty("inqNum")
    private String inqNum;                  // 조회수

    @JsonProperty("lastModYmd")
    private String lastModYmd;              // 최종수정일
}