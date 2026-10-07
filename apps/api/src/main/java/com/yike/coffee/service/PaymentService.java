package com.yike.coffee.service;

/** 支付扩展点；MVP 仅使用线下/演示支付，不发起真实交易。 */
public interface PaymentService {
    String provider();
}
