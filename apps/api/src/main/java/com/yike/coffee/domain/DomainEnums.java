package com.yike.coffee.domain;

public final class DomainEnums {
    private DomainEnums() {}
    public enum Role { PLATFORM_ADMIN, MERCHANT_ADMIN, CUSTOMER }
    public enum MerchantStatus { PENDING, APPROVED, REJECTED, SUSPENDED }
    public enum StoreStatus { OPEN, CLOSED, DISABLED }
    public enum ProductStatus { DRAFT, ON_SALE, OFF_SALE }
    public enum OrderStatus { PENDING_ACCEPTANCE, PREPARING, READY, COMPLETED, REJECTED, CANCELLED }
}
