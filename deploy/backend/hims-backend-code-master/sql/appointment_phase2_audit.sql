-- 阶段 2：持久化幂等与有效预约约束——迁移前置只读审计脚本
-- 用途：在 appointment_phase2.sql 之前执行，评估历史数据是否满足新增约束的前置条件。
-- 本脚本只做 SELECT 查询，不修改、不删除任何历史数据。
-- 执行前请先备份database（建议 mysqldump 全库或至少 appointments_info / order_info / duty_info / patients_info）。
--
-- 硬性门槛：第 1 项「重复有效预约」必须为 0 行，否则 ALTER TABLE 增加唯一索引会失败；
--           若不为 0 行，必须先由业务方人工裁定保留哪一条（禁止脚本自动删除）。
-- 其余各项为人工评估项，不阻断迁移，但需在实施记录中登记结论。

-- 0. 版本探测：生成列需 MySQL 5.7.6+，本方案按 5.7.18 设计。
--    若结果 < 5.7.6，生成列语法不可用，必须改用「独立活动预约占位表」方案，停止执行本迁移。
SELECT VERSION() AS mysql_version;

-- 1.【硬性门槛】重复有效预约：同一 (schedule_id, patient_id) 存在多条有效预约
--    有效 = del_flag = '0' AND status IN ('0','1')，与迁移后生成列 active_patient_id 的语义一致。
--    期望结果：0 行。非 0 行必须先人工清理，否则唯一索引创建失败。
SELECT a.schedule_id,
       a.patient_id,
       COUNT(*)                     AS active_count,
       GROUP_CONCAT(a.appointments_id ORDER BY a.appointments_id) AS appointments_ids
FROM appointments_info a
WHERE a.del_flag = '0'
  AND a.status IN ('0', '1')
  AND a.schedule_id IS NOT NULL
  AND a.patient_id IS NOT NULL
GROUP BY a.schedule_id, a.patient_id
HAVING active_count > 1
ORDER BY active_count DESC, a.schedule_id;

-- 2. 孤立预约：排班不存在或已删除
SELECT a.appointments_id, a.schedule_id, a.patient_id, a.status, a.del_flag
FROM appointments_info a
LEFT JOIN duty_info d ON d.schedule_id = a.schedule_id
WHERE d.schedule_id IS NULL OR d.del_flag <> '0'
ORDER BY a.appointments_id;

-- 3. 孤立订单：订单指向的预约不存在
SELECT o.order_id, o.appointments_id, o.user_id, o.pay_state
FROM order_info o
LEFT JOIN appointments_info a ON a.appointments_id = o.appointments_id
WHERE a.appointments_id IS NULL
ORDER BY o.order_id;

-- 4. 号源计数不一致：duty_info.appointments_num_made 与有效预约数量不符
--    差异说明：阶段 1 之前的旧入口未经过统一事务，可能已造成历史偏差。
SELECT d.schedule_id,
       d.appointments_num                                   AS capacity,
       d.appointments_num_made                              AS num_made,
       COALESCE(v.active_count, 0)                          AS active_appointments,
       d.appointments_num_made - COALESCE(v.active_count, 0) AS diff
FROM duty_info d
LEFT JOIN (
    SELECT schedule_id, COUNT(*) AS active_count
    FROM appointments_info
    WHERE del_flag = '0' AND status IN ('0', '1')
    GROUP BY schedule_id
) v ON v.schedule_id = d.schedule_id
WHERE d.del_flag = '0'
  AND d.appointments_num_made <> COALESCE(v.active_count, 0)
ORDER BY ABS(d.appointments_num_made - COALESCE(v.active_count, 0)) DESC;

-- 5. 未知预约状态：不在已知集合 {'0','1','2','3'} 内
--    已知语义：0 待支付、1 已支付（两者参与有效预约唯一性判定）、
--              2 已取消、3 已退款（两者不参与唯一性判定）。
--    注意：状态只决定是否参与唯一性判定，与 duty_info.appointments_num_made 是否回退无关。
SELECT a.status, COUNT(*) AS cnt, GROUP_CONCAT(a.appointments_id ORDER BY a.appointments_id) AS appointments_ids
FROM appointments_info a
WHERE a.status IS NULL OR a.status NOT IN ('0', '1', '2', '3')
GROUP BY a.status;

-- 6. 未知订单状态：不在已知集合 {'0','1','2','3'} 内
--    已知语义：0 待支付、1 已支付、2 已取消、3 已退款（见 Constants.java）。
SELECT o.pay_state, COUNT(*) AS cnt, GROUP_CONCAT(o.order_id ORDER BY o.order_id) AS order_ids
FROM order_info o
WHERE o.pay_state IS NULL OR o.pay_state NOT IN ('0', '1', '2', '3')
GROUP BY o.pay_state;

-- 7. 幂等唯一约束冲突风险
--    appointment_idempotent 为阶段 2 新建表，无历史数据，主键 (user_id, request_id) 不存在存量冲突。
--    此处仅确认同名对象是否已存在，避免重复建表。
--    期望结果：0 行。
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'appointment_idempotent';

-- 7.1 生成列 / 唯一索引是否已存在（重复执行迁移的保护性检查）
--     期望结果：0 行。
SELECT table_name, column_name, generation_expression
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'appointments_info'
  AND column_name = 'active_patient_id';

-- 8. 患者关系缺失：存在有效预约，但预约患者未建立任何用户归属关系
--    这类数据无法通过统一事务 Service 校验，需人工确认后补建 user_patient_rel。
SELECT a.appointments_id, a.patient_id, a.schedule_id, a.status
FROM appointments_info a
LEFT JOIN user_patient_rel r ON r.patient_id = a.patient_id
WHERE a.del_flag = '0'
  AND a.status IN ('0', '1')
  AND r.patient_id IS NULL
ORDER BY a.appointments_id;
