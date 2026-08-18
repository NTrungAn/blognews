package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.RefreshToken;
import com.blog.blogsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Tìm refresh token hợp lệ (chưa bị revoke) theo hash.
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * Lấy tất cả refresh token của một user, sắp xếp theo thời gian tạo giảm dần.
     */
    List<RefreshToken> findAllByUserAndRevokedFalseOrderByCreatedAtDesc(User user);

    /**
     * Revoke tất cả refresh token của một user (dùng khi logout).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllByUser(@Param("user") User user);

    /**
     * Đếm số refresh token đang active (chưa revoke, chưa hết hạn) của user.
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > CURRENT_TIMESTAMP")
    long countActiveByUser(@Param("user") User user);

    /**
     * Tìm refresh token theo hash, BẤT KỂ trạng thái revoke.
     * Dùng để phát hiện Token Reuse Attack.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoke toàn bộ token family (dùng khi phát hiện reuse attack).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId AND rt.revoked = false")
    int revokeAllByFamilyId(@Param("familyId") UUID familyId);

    /**
     * Xóa tất cả token đã hết hạn (dùng cho scheduled cleanup).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
