-- =============================================================================
-- 阶段 2 回滚：撤销持久化幂等表与有效预约唯一约束
-- 目标库：MySQL 5.7.18。
-- 执行顺序与 appointment_phase2.sql 严格相反：先删索引 → 再删生成列 → 最后删幂等表。
-- 索引依赖生成列，必须先删索引，否则 DROP COLUMN 会因索引引用而失败。
-- =============================================================================
--
-- 【回滚前确认】
--   1) 应用已停止或已回退到阶段 1 版本（阶段 2 代码依赖 active_patient_id 列与 appointment_idempotent 表，
--      列被删除后阶段 2 代码会直接报 SQL 错误）。
--   2) 确认无在途客户端持有未完成的 requestId 重试；DROP TABLE 后这些重试将被当作全新请求再次占号。
--   3) 建议回滚前再次备份 appointments_info 与 appointment_idempotent。
--
-- 【不可逆】
--   1) DROP COLUMN active_patient_id 后，生成列定义与唯一索引同时消失，无法从表结构恢复；
--      如需再次启用，重新执行 appointment_phase2.sql 步骤 2、3。
--   2) DROP TABLE appointment_idempotent 会永久丢失幂等记录。此后同一 requestId 的重试不再返回原结果，
--      而会作为新请求重新走一遍下单流程（可能重复占号）。此部分不可逆，只能以备份恢复。
--   3) 本脚本不修改任何业务数据行；但步骤 2 的 DROP COLUMN 在 MySQL 5.7 下同样为 ALGORITHM=COPY，
--      会重建 appointments_info 并锁写，需在维护窗口执行。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 步骤 1：删除有效预约唯一索引
-- -----------------------------------------------------------------------------
ALTER TABLE appointments_info
    DROP INDEX uk_active_schedule_patient;

-- -----------------------------------------------------------------------------
-- 步骤 2：删除有效预约生成列
-- -----------------------------------------------------------------------------
ALTER TABLE appointments_info
    DROP COLUMN active_patient_id;

-- -----------------------------------------------------------------------------
-- 步骤 3：删除幂等记录表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS appointment_idempotent;

-- -----------------------------------------------------------------------------
-- 回滚后自检（只读）：两项均应返回 0 行
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS remaining_active_patient_id_column
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'appointments_info'
  AND column_name = 'active_patient_id';

SELECT COUNT(*) AS remaining_idempotent_table
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'appointment_idempotent';
