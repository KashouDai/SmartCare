-- =============================================================================
-- 阶段 2 迁移：持久化幂等表 + 有效预约唯一约束
-- 目标库：MySQL 5.7.18（腾讯云 CynosDB），语法为 MySQL 5.7 可执行语法。
-- 本脚本不是 H2 专用语法；测试用 H2 schema 见 hims-order/src/test/resources/registered_order_test_schema.sql。
-- =============================================================================
--
-- 【执行前备份】
--   必须先行备份，建议至少：
--     mysqldump -u<user> -p himsss appointments_info duty_info order_info patients_info user_patient_rel > backup_before_phase2.sql
--   本脚本会重建 appointments_info（见下方步骤 2 说明），无备份不可执行。
--
-- 【前置审计条件】
--   必须先执行 appointment_phase2_audit.sql，并满足：
--     1) 第 0 项版本探测结果 >= 5.7.6（否则生成列语法不可用，本方案不成立，改用独立占位表方案）
--     2) 第 1 项「重复有效预约」为 0 行（硬性门槛，否则步骤 3 的唯一索引创建必然失败）
--     3) 第 7 / 7.1 项为 0 行（确认同名对象不存在，避免重复执行报错）
--   其余审计项为人工评估项，需在实施记录中登记结论。
--
-- 【执行顺序】严格按 1 → 2 → 3 顺序执行，不可打乱。
--
-- 【失败处理】
--   任一步骤报错即停止，不要继续执行后续步骤；先执行 appointment_phase2_rollback.sql 回到迁移前状态，
--   定位原因（通常是审计第 1 项未清零或版本低于 5.7.6）后重新走审计 → 迁移流程。
--
-- 【回滚】见 appointment_phase2_rollback.sql。
--
-- 【不可逆部分】
--   1) 步骤 2 为 STORED 生成列，MySQL 5.7 下只能使用 ALGORITHM=COPY，会重建 appointments_info 表并
--      全程锁写。必须安排在维护窗口内执行；大表上耗时与磁盘占用显著，执行前确认磁盘余量。
--      该步骤本身可通过 rollback 脚本回退，但重建过程不可中断恢复。
--   2) 步骤 1 建表后若已产生幂等记录，DROP TABLE appointment_idempotent 会使历史 requestId 失去
--      重放语义（客户端同 requestId 重试将被当作全新请求再次占号）。回滚前需确认无在途客户端重试。
--   3) 回滚不会恢复因迁移期间报错而半途产生的数据变更；以备份为准。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 步骤 1：幂等记录表
-- 主键 (user_id, request_id) 即需求的 (user_id, request_id) 唯一约束，
-- 沿用阶段 1 user_patient_rel 的复合主键风格，不引入代理主键。
-- 字符集 utf8mb4 与阶段 1 新建表一致（主库为 utf8，差异已在实施记录中登记）。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointment_idempotent (
    user_id             BIGINT       NOT NULL                COMMENT '登录用户 ID',
    request_id          VARCHAR(64)  NOT NULL                COMMENT '客户端幂等请求 ID',
    business_type       VARCHAR(32)  NOT NULL                COMMENT '业务类型，当前为 REGISTERED_ORDER',
    request_fingerprint VARCHAR(64)  NOT NULL                COMMENT '请求指纹，SHA-256 hex，覆盖业务类型/排班/患者',
    appointments_id     BIGINT       NULL                    COMMENT '成功后的预约 ID',
    order_id            BIGINT       NULL                    COMMENT '成功后的订单 ID',
    result_status       VARCHAR(16)  NOT NULL                COMMENT '结果状态，当前为 SUCCESS',
    create_time         DATETIME     NOT NULL                COMMENT '创建时间',
    update_time         DATETIME     NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (user_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约挂号幂等记录';

-- -----------------------------------------------------------------------------
-- 步骤 2：有效预约生成列
-- 语义：del_flag = '0' 且 status IN ('0','1') 时取 patient_id，其他状态取 NULL。
--   0 待支付、1 已支付 → 参与唯一性判定，生成列取 patient_id
--   2 已取消、3 已退款 → 不参与唯一性判定，生成列为 NULL
-- 取消/退款后生成列自动变 NULL，该患者可再次预约同一排班，无需删除任何行，也无需应用层同步。
--
-- 【重要边界】本生成列只解除「唯一约束层面的占位」，不释放号源计数：
--   duty_info.appointments_num_made 由阶段 1 的原子占号语句只增不减，
--   取消/退款不会使其回退。真正的号源释放必须由取消/退款路径显式实现，不在本阶段范围。
-- 生成列为 STORED：表达式结果落盘，可参与索引；MySQL 5.7 下本语句为 ALGORITHM=COPY。
-- -----------------------------------------------------------------------------
ALTER TABLE appointments_info
    ADD COLUMN active_patient_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN del_flag = '0' AND status IN ('0', '1')
                     AND schedule_id IS NOT NULL AND patient_id IS NOT NULL
                THEN patient_id
                ELSE NULL
            END
        ) STORED
        COMMENT '有效预约唯一占位患者ID：del_flag=0 且 status in (0,1) 时为 patient_id，否则 NULL；不表示号源计数';

-- -----------------------------------------------------------------------------
-- 步骤 3：有效预约唯一索引
-- 唯一索引建在 (schedule_id, active_patient_id) 上，由database仲裁
-- 「同一排班 + 同一患者最多一条有效预约」。
-- 唯一索引允许多个 NULL，故非有效预约（生成列为 NULL）不受限制。
-- 不建永久 (schedule_id, patient_id) 唯一索引：那会使取消/退款后无法重新预约，改变业务行为。
-- 注意：本索引只约束预约记录的重复，不代表号源计数一致，号源一致性由审计脚本第 4 项单独核查。
-- -----------------------------------------------------------------------------
ALTER TABLE appointments_info
    ADD UNIQUE KEY uk_active_schedule_patient (schedule_id, active_patient_id);

-- -----------------------------------------------------------------------------
-- 迁移后自检（只读）
-- 1) 生成列与唯一索引已就位
-- 2) 有效预约数量与迁移前一致（防止步骤 2 重建过程异常）
-- -----------------------------------------------------------------------------
SELECT column_name, generation_expression, extra
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'appointments_info'
  AND column_name = 'active_patient_id';

SELECT index_name, non_unique, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'appointments_info'
  AND index_name = 'uk_active_schedule_patient'
GROUP BY index_name, non_unique;

SELECT COUNT(*) AS active_appointments_after_migration
FROM appointments_info
WHERE del_flag = '0' AND status IN ('0', '1');
