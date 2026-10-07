package com.yike.coffee.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yike.coffee.api.ApiException;
import com.yike.coffee.domain.DomainModels.User;
import com.yike.coffee.mapper.MerchantMemberMapper;
import com.yike.coffee.mapper.UserMapper;
import com.yike.coffee.security.AppPrincipal;
import com.yike.coffee.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final UserMapper users;
    private final MerchantMemberMapper members;
    private final JdbcTemplate jdbc;
    private final JwtService jwt;
    private final boolean devEnabled;
    private final long refreshDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserMapper users, MerchantMemberMapper members, JdbcTemplate jdbc, JwtService jwt,
                       @Value("${app.auth.dev-enabled:false}") boolean devEnabled,
                       @Value("${app.auth.refresh-days:7}") long refreshDays) {
        this.users = users; this.members = members; this.jdbc = jdbc; this.jwt = jwt;
        this.devEnabled = devEnabled; this.refreshDays = refreshDays;
    }

    @Transactional
    public Map<String, Object> devLogin(String email) {
        if (!devEnabled) throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "接口不存在");
        User user = users.selectOne(Wrappers.<User>query().eq("email", email).eq("enabled", true));
        if (user == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "测试账号不存在或已停用");
        return issue(principal(user));
    }

    @Transactional
    public Map<String, Object> refresh(String rawToken) {
        var rows = jdbc.query("SELECT u.id,u.email,u.display_name,u.role,u.enabled FROM refresh_session s JOIN app_user u ON u.id=s.user_id " +
                "WHERE s.token_hash=? AND s.revoked_at IS NULL AND s.expires_at>NOW()", (rs, n) -> {
            User u = new User(); u.id=rs.getString(1); u.email=rs.getString(2); u.displayName=rs.getString(3);
            u.role=rs.getString(4); u.enabled=rs.getBoolean(5); return u;
        }, hash(rawToken));
        if (rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).enabled))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID", "刷新令牌无效或已过期");
        jdbc.update("UPDATE refresh_session SET revoked_at=NOW(3) WHERE token_hash=?", hash(rawToken));
        return issue(principal(rows.get(0)));
    }

    public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank())
            jdbc.update("UPDATE refresh_session SET revoked_at=NOW(3) WHERE token_hash=? AND revoked_at IS NULL", hash(rawToken));
    }

    private AppPrincipal principal(User user) {
        var member = members.selectOne(Wrappers.<com.yike.coffee.domain.DomainModels.MerchantMember>query()
            .eq("user_id", user.id).last("LIMIT 1"));
        return new AppPrincipal(user.id, user.email, user.displayName, user.role, member == null ? null : member.merchantId);
    }

    private Map<String, Object> issue(AppPrincipal principal) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO refresh_session(id,user_id,token_hash,expires_at) VALUES(?,?,?,?)",
            UUID.randomUUID().toString(), principal.userId(), hash(refresh), Timestamp.from(Instant.now().plus(refreshDays, ChronoUnit.DAYS)));
        return Map.of("accessToken", jwt.create(principal), "refreshToken", refresh, "expiresIn", 7200,
            "user", Map.of("id", principal.userId(), "email", principal.email(), "displayName", principal.displayName(),
                "role", principal.role(), "merchantId", principal.merchantId() == null ? "" : principal.merchantId()));
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
