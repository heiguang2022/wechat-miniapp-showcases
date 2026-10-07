package com.yike.coffee.service;

import java.util.Map;

/** 正式微信登录替换点：实现 code2Session 后返回与开发登录相同的令牌结构。 */
public interface WechatLoginProvider {
    Map<String,Object> login(String code);
}
