package org.benefitmap.backend.auth;

import jakarta.persistence.*;
import lombok.*;
import org.benefitmap.backend.user.User;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_token",
        uniqueConstraints = @UniqueConstraint(name="uk_refresh_token_hash", columnNames = "token_hash"),
        indexes = @Index(name="idx_refresh_expires", columnList = "expires_at"))
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_refresh_user"))
    private User user;

    // 반드시 VARCHAR(64) 이어야 함 (DB도 동일)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
