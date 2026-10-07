package com.yike.coffee.security;

import com.yike.coffee.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}
    public static AppPrincipal required() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AppPrincipal app) return app;
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
    }
    public static String merchantId() {
        String id = required().merchantId();
        if (id == null) throw new ApiException(HttpStatus.FORBIDDEN, "MERCHANT_REQUIRED", "当前账号未关联商家");
        return id;
    }
}
