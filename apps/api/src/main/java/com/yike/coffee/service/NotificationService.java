package com.yike.coffee.service;

/** 通知扩展点；后续可接微信订阅消息。 */
public interface NotificationService {
    void orderStatusChanged(String orderId, String status);
}
