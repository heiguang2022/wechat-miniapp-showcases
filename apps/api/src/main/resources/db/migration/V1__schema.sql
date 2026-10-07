CREATE TABLE app_user (
  id CHAR(36) PRIMARY KEY,
  email VARCHAR(191) NOT NULL UNIQUE,
  display_name VARCHAR(80) NOT NULL,
  role VARCHAR(32) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE wechat_identity (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  open_id VARCHAR(128) NOT NULL UNIQUE,
  union_id VARCHAR(128),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_wechat_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE merchant (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reject_reason VARCHAR(255),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE merchant_member (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_merchant_member (merchant_id, user_id),
  CONSTRAINT fk_member_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
  CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE refresh_session (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE store (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36) NOT NULL,
  name VARCHAR(120) NOT NULL,
  address VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  business_hours VARCHAR(80) NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_store_merchant (merchant_id),
  CONSTRAINT fk_store_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id)
);

CREATE TABLE category (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36) NOT NULL,
  name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_category_name (merchant_id, name),
  CONSTRAINT fk_category_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id)
);

CREATE TABLE product (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36) NOT NULL,
  category_id CHAR(36) NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500),
  base_price BIGINT NOT NULL,
  image_url VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_product_merchant (merchant_id),
  CONSTRAINT fk_product_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE product_image (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_image_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
  CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE spec_group (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36) NOT NULL,
  name VARCHAR(80) NOT NULL,
  required_flag BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_spec_group_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id)
);

CREATE TABLE spec_option (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36) NOT NULL,
  group_id CHAR(36) NOT NULL,
  name VARCHAR(80) NOT NULL,
  price_delta BIGINT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_spec_option_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
  CONSTRAINT fk_spec_option_group FOREIGN KEY (group_id) REFERENCES spec_group(id)
);

CREATE TABLE product_spec_group (
  product_id CHAR(36) NOT NULL,
  group_id CHAR(36) NOT NULL,
  merchant_id CHAR(36) NOT NULL,
  PRIMARY KEY (product_id, group_id),
  CONSTRAINT fk_psg_product FOREIGN KEY (product_id) REFERENCES product(id),
  CONSTRAINT fk_psg_group FOREIGN KEY (group_id) REFERENCES spec_group(id),
  CONSTRAINT fk_psg_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id)
);

CREATE TABLE orders (
  id CHAR(36) PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL UNIQUE,
  idempotency_key VARCHAR(80) NOT NULL,
  merchant_id CHAR(36) NOT NULL,
  store_id CHAR(36) NOT NULL,
  customer_id CHAR(36) NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_amount BIGINT NOT NULL,
  item_count INT NOT NULL,
  payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
  payment_provider VARCHAR(32) NOT NULL DEFAULT 'OFFLINE',
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_customer_idempotency (customer_id, idempotency_key),
  KEY idx_order_merchant (merchant_id, created_at),
  KEY idx_order_customer (customer_id, created_at),
  CONSTRAINT fk_order_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
  CONSTRAINT fk_order_store FOREIGN KEY (store_id) REFERENCES store(id),
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES app_user(id)
);

CREATE TABLE order_item (
  id CHAR(36) PRIMARY KEY,
  order_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  unit_price BIGINT NOT NULL,
  quantity INT NOT NULL,
  line_amount BIGINT NOT NULL,
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE order_item_option (
  id CHAR(36) PRIMARY KEY,
  order_item_id CHAR(36) NOT NULL,
  option_id CHAR(36) NOT NULL,
  group_name VARCHAR(80) NOT NULL,
  option_name VARCHAR(80) NOT NULL,
  price_delta BIGINT NOT NULL,
  CONSTRAINT fk_item_option_item FOREIGN KEY (order_item_id) REFERENCES order_item(id),
  CONSTRAINT fk_item_option_option FOREIGN KEY (option_id) REFERENCES spec_option(id)
);

CREATE TABLE order_status_log (
  id CHAR(36) PRIMARY KEY,
  order_id CHAR(36) NOT NULL,
  operator_id CHAR(36) NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_status_log_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_status_log_operator FOREIGN KEY (operator_id) REFERENCES app_user(id)
);

CREATE TABLE upload_file (
  id CHAR(36) PRIMARY KEY,
  merchant_id CHAR(36),
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(120) NOT NULL UNIQUE,
  content_type VARCHAR(80) NOT NULL,
  size_bytes BIGINT NOT NULL,
  public_url VARCHAR(500) NOT NULL,
  created_by CHAR(36) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_upload_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
  CONSTRAINT fk_upload_user FOREIGN KEY (created_by) REFERENCES app_user(id)
);

CREATE TABLE audit_log (
  id CHAR(36) PRIMARY KEY,
  operator_id CHAR(36) NOT NULL,
  action VARCHAR(80) NOT NULL,
  target_type VARCHAR(80) NOT NULL,
  target_id CHAR(36) NOT NULL,
  details VARCHAR(1000),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_audit_created (created_at),
  CONSTRAINT fk_audit_operator FOREIGN KEY (operator_id) REFERENCES app_user(id)
);
