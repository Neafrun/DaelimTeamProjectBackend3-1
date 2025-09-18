package org.benefitmap.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 로그아웃(현재 기기) 용: 토큰 해시로 1건 삭제
    void deleteByTokenHash(String tokenHash);

    // 회원탈퇴/전체 로그아웃 용: 해당 사용자의 모든 토큰 삭제
    void deleteByUser_Id(Long userId);
}
