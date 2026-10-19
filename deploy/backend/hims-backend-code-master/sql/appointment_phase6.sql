-- 阶段 6：支付截止时间与生命周期条件更新
-- 目标库：MySQL 5.7；重复执行前请确认列和索引是否已存在。

ALTER TABLE order_info
    ADD COLUMN payment_deadline datetime NULL COMMENT '支付截止时间' AFTER pay_state;

ALTER TABLE order_info
    ADD INDEX idx_order_pending_deadline (pay_state, payment_deadline);

