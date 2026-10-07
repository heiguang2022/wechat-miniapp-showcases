package com.yike.coffee.service;

import com.yike.coffee.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class UnconfiguredWechatLoginProvider implements WechatLoginProvider {
    @Override public Map<String,Object> login(String code) {
        throw new ApiException(HttpStatus.NOT_IMPLEMENTED,"WECHAT_NOT_CONFIGURED","尚未配置正式微信 AppID，请使用开发登录");
    }
}
