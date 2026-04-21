-- ============================================================
-- 微信小程序拼团商城 - 数据库初始化脚本
-- 数据库：shop_db
-- 版本：v2.0  日期：2026-04-10（与 Java 实体类对齐）
-- ============================================================

CREATE DATABASE IF NOT EXISTS shop_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE shop_db;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_user
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    openid     VARCHAR(64)  NOT NULL UNIQUE COMMENT '微信openid',
    nickname   VARCHAR(64)           COMMENT '昵称',
    avatar_url VARCHAR(255)          COMMENT '头像URL',
    phone      VARCHAR(20)           COMMENT '手机号',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- ============================================================
-- 2. 商品分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_product_category
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name       VARCHAR(32)  NOT NULL COMMENT '分类名称',
    icon       VARCHAR(255)          COMMENT '分类图标',
    sort       INT          NOT NULL DEFAULT 0 COMMENT '排序（越大越前）',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类表';

-- ============================================================
-- 3. 商品表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_product
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    category_id        BIGINT                COMMENT '分类ID',
    name               VARCHAR(128) NOT NULL COMMENT '商品名称',
    description        TEXT                  COMMENT '商品描述',
    cover_img          VARCHAR(255)          COMMENT '封面主图',
    images             JSON                  COMMENT '商品图片列表',
    price              DECIMAL(10, 2) NOT NULL COMMENT '售价（元）',
    original_price     DECIMAL(10, 2)        COMMENT '原价（元）',
    group_price        DECIMAL(10, 2)        COMMENT '拼团价（NULL=不支持拼团）',
    stock              INT          NOT NULL DEFAULT 0 COMMENT '库存数量',
    sales              INT          NOT NULL DEFAULT 0 COMMENT '销量',
    status             TINYINT      NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
    group_required     INT                   COMMENT '拼团人数（NULL=用全局配置）',
    group_expire_hours INT                   COMMENT '拼团时限小时（NULL=用全局配置）',
    version            INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- ============================================================
-- 4. 订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_order
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no       VARCHAR(32)    NOT NULL UNIQUE COMMENT '订单号',
    user_id        BIGINT         NOT NULL COMMENT '用户ID',
    product_id     BIGINT         NOT NULL COMMENT '商品ID',
    product_name   VARCHAR(128)            COMMENT '商品名称快照',
    product_image  VARCHAR(255)            COMMENT '商品图片快照',
    unit_price     DECIMAL(10, 2)          COMMENT '单价快照（元）',
    quantity       INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount   DECIMAL(10, 2) NOT NULL COMMENT '订单总金额（元）',
    pay_amount     DECIMAL(10, 2)          COMMENT '实付金额（元）',
    status         TINYINT        NOT NULL DEFAULT 0 COMMENT '0=待付款 1=待发货 2=已发货 3=已完成 4=申请退款 5=退款中 6=已退款 7=已取消',
    type           TINYINT        NOT NULL DEFAULT 1 COMMENT '1=普通订单 2=拼团订单',
    is_group       TINYINT        NOT NULL DEFAULT 0 COMMENT '0=普通 1=拼团',
    group_id       BIGINT                  COMMENT '关联拼团ID',
    address_info   JSON                    COMMENT '收货地址快照',
    pay_time       DATETIME                COMMENT '支付时间',
    transaction_id VARCHAR(64)             COMMENT '微信支付交易号',
    ship_company   VARCHAR(64)             COMMENT '物流公司',
    ship_no        VARCHAR(64)             COMMENT '物流单号',
    shipped_at     DATETIME                COMMENT '发货时间',
    received_at    DATETIME                COMMENT '确认收货时间',
    remark         VARCHAR(255)            COMMENT '备注',
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_order_no (order_no),
    INDEX idx_group (group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

-- ============================================================
-- 5. 拼团活动表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_group
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '拼团ID',
    group_code         VARCHAR(32)    NOT NULL UNIQUE COMMENT '拼团码（8位，分享用）',
    product_id         BIGINT         NOT NULL COMMENT '商品ID',
    product_name       VARCHAR(128)            COMMENT '商品名称快照',
    product_image      VARCHAR(255)            COMMENT '商品主图快照',
    group_price        DECIMAL(10, 2)          COMMENT '拼团价快照',
    initiator_id       BIGINT         NOT NULL COMMENT '发起人用户ID',
    initiator_order_id BIGINT         NOT NULL COMMENT '发起人订单ID',
    required_count     INT            NOT NULL DEFAULT 3 COMMENT '需要参团总人数',
    current_count      INT            NOT NULL DEFAULT 1 COMMENT '当前参团人数（含发起人）',
    status             TINYINT        NOT NULL DEFAULT 1 COMMENT '1=进行中 2=成团 3=过期失败 4=已关闭',
    expire_at          DATETIME       NOT NULL COMMENT '过期时间',
    completed_at       DATETIME                COMMENT '成团时间',
    created_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (group_code),
    INDEX idx_initiator (initiator_id),
    INDEX idx_status (status),
    INDEX idx_expire (expire_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '拼团活动表';

-- ============================================================
-- 6. 拼团成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_group_member
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员ID',
    group_id  BIGINT       NOT NULL COMMENT '拼团ID',
    user_id   BIGINT       NOT NULL COMMENT '参团用户ID',
    nickname  VARCHAR(64)           COMMENT '用户昵称快照',
    avatar    VARCHAR(255)          COMMENT '用户头像快照',
    order_id  BIGINT       NOT NULL COMMENT '参团订单ID',
    role      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=普通成员 1=发起人',
    joined_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group (group_id),
    INDEX idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '拼团成员表';

-- ============================================================
-- 7. 支付记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_pay_record
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    order_id       BIGINT         NOT NULL COMMENT '订单ID',
    user_id        BIGINT         NOT NULL COMMENT '用户ID',
    out_trade_no   VARCHAR(32)    NOT NULL UNIQUE COMMENT '商户支付单号',
    transaction_id VARCHAR(64)             COMMENT '微信交易号',
    amount         DECIMAL(10, 2) NOT NULL COMMENT '支付金额（元）',
    status         TINYINT        NOT NULL DEFAULT 0 COMMENT '0=待支付 1=已支付 2=已退款',
    paid_at        DATETIME                COMMENT '支付成功时间',
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    INDEX idx_out_trade_no (out_trade_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付记录表';

-- ============================================================
-- 8. 退款记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_refund
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '退款ID',
    out_refund_no VARCHAR(32)    NOT NULL UNIQUE COMMENT '商户退款单号',
    refund_id     VARCHAR(64)             COMMENT '微信退款单号',
    order_id      BIGINT         NOT NULL COMMENT '原订单ID',
    user_id       BIGINT         NOT NULL COMMENT '申请用户ID',
    amount        DECIMAL(10, 2) NOT NULL COMMENT '退款金额（元）',
    reason        VARCHAR(255)            COMMENT '退款原因',
    status        TINYINT        NOT NULL DEFAULT 0 COMMENT '0=处理中 1=成功 2=失败',
    refunded_at   DATETIME                COMMENT '退款完成时间',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order (order_id),
    INDEX idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '退款记录表';

-- ============================================================
-- 9. 管理员表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_admin
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID',
    username   VARCHAR(32)  NOT NULL UNIQUE COMMENT '用户名',
    password   VARCHAR(128) NOT NULL COMMENT '密码（BCrypt）',
    name       VARCHAR(32)           COMMENT '真实姓名',
    role       VARCHAR(32)  NOT NULL DEFAULT 'admin' COMMENT '角色',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员表';

-- ============================================================
-- 10. 系统配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS t_sys_config
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64)  NOT NULL UNIQUE COMMENT '配置键',
    config_val VARCHAR(512) NOT NULL COMMENT '配置值',
    remark     VARCHAR(255)          COMMENT '备注',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统配置表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 默认管理员（密码：admin123，BCrypt加密）
INSERT IGNORE INTO t_admin (username, password, name, role)
VALUES ('admin', '$2a$10$THOX4T3GEJ7qIB9Qfzn4QeYlzTt9ttyAl7ge0DZk2S127/ws6rfiK', '超级管理员', 'superadmin');

-- 默认商品分类
INSERT IGNORE INTO t_product_category (id, name, sort)
VALUES (1, '水果生鲜', 100),
       (2, '零食饮料', 90),
       (3, '日用百货', 80),
       (4, '数码配件', 70);

-- 测试商品（支持拼团）
INSERT IGNORE INTO t_product (id, category_id, name, description, cover_img, price, original_price, group_price, stock, status)
VALUES (1, 1, '新疆阿克苏苹果5斤装', '皮薄汁多，甜脆爽口', 'https://example.com/apple.jpg', 29.90, 49.90, 19.90, 1000, 1),
       (2, 2, '百草味坚果大礼包', '混合坚果，营养美味', 'https://example.com/nuts.jpg', 59.90, 89.90, 39.90, 500, 1);

-- 系统配置
INSERT IGNORE INTO t_sys_config (config_key, config_val, remark)
VALUES ('group_required_count', '3', '拼团默认所需人数'),
       ('group_expire_hours', '24', '拼团默认有效小时数'),
       ('order_pay_timeout_minutes', '30', '拼团订单支付超时分钟数');
