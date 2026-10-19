-- 阶段 1：用户—患者归属关系只读扫描脚本
-- 用途：迁移与上线前评估历史患者归属，仅供人工核对，脚本不修改任何数据。
-- 执行前请先备份database；禁止依据姓名或手机号自动建立归属关系。

-- 1. 已建立的用户—患者关系总数
SELECT COUNT(*) AS relation_count FROM user_patient_rel;

-- 2. 尚无任何归属关系的患者（需要人工确认，禁止按姓名或手机号自动归属）
SELECT p.patient_id, p.nick_name, p.phone, p.card_no, p.del_flag
FROM patients_info p
LEFT JOIN user_patient_rel r ON r.patient_id = p.patient_id
WHERE r.patient_id IS NULL
ORDER BY p.patient_id;

-- 3. 仍指向已删除患者的关系记录（当前策略为保留关系，仅过滤 del_flag 查询，需人工确认）
SELECT r.user_id, r.patient_id, p.del_flag
FROM user_patient_rel r
INNER JOIN patients_info p ON p.patient_id = r.patient_id
WHERE p.del_flag <> '0'
ORDER BY r.patient_id;

-- 4. 手机号重复的存活患者（仅报告，不自动归属）
SELECT p.phone, COUNT(*) AS patient_count, GROUP_CONCAT(p.patient_id) AS patient_ids
FROM patients_info p
WHERE p.del_flag = '0' AND p.phone IS NOT NULL AND p.phone <> ''
GROUP BY p.phone
HAVING patient_count > 1;
