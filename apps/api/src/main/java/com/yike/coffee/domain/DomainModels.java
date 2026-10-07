package com.yike.coffee.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;

/** MVP 数据实体集中定义，避免实体携带业务逻辑。 */
public final class DomainModels {
    private DomainModels() {}

    @TableName("app_user")
    public static class User {
        @TableId public String id;
        public String email;
        public String displayName;
        public String role;
        public Boolean enabled;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    @TableName("merchant")
    public static class Merchant {
        @TableId public String id;
        public String name;
        public String status;
        public String rejectReason;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    @TableName("merchant_member")
    public static class MerchantMember {
        @TableId public String id;
        public String merchantId;
        public String userId;
        public LocalDateTime createdAt;
    }

    @TableName("store")
    public static class Store {
        @TableId public String id;
        public String merchantId;
        public String name;
        public String address;
        public String status;
        public String businessHours;
        @TableLogic public Boolean deleted;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    @TableName("category")
    public static class Category {
        @TableId public String id;
        public String merchantId;
        public String name;
        public Integer sortOrder;
        public Boolean enabled;
        public LocalDateTime createdAt;
    }

    @TableName("product")
    public static class Product {
        @TableId public String id;
        public String merchantId;
        public String categoryId;
        public String name;
        public String description;
        public Long basePrice;
        public String imageUrl;
        public String status;
        public Integer sortOrder;
        @TableLogic public Boolean deleted;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    @TableName("spec_group")
    public static class SpecGroup {
        @TableId public String id;
        public String merchantId;
        public String name;
        public Boolean requiredFlag;
        public LocalDateTime createdAt;
    }

    @TableName("spec_option")
    public static class SpecOption {
        @TableId public String id;
        public String merchantId;
        public String groupId;
        public String name;
        public Long priceDelta;
        public Boolean enabled;
        public Integer sortOrder;
    }

    @TableName("orders")
    public static class Order {
        @TableId public String id;
        public String orderNo;
        public String idempotencyKey;
        public String merchantId;
        public String storeId;
        public String customerId;
        public String status;
        public Long totalAmount;
        public Integer itemCount;
        public String paymentStatus;
        public String paymentProvider;
        @Version public Integer version;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }
}
