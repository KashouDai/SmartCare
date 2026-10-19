-- 阶段 1：患者与登录用户关系表
-- 执行前请先备份database，并完成历史患者归属审计。
CREATE TABLE IF NOT EXISTS user_patient_rel (
    user_id BIGINT NOT NULL COMMENT '登录用户 ID',
    patient_id BIGINT NOT NULL COMMENT '患者 ID',
    relation_type VARCHAR(32) NOT NULL DEFAULT 'OWNER' COMMENT '关系类型',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, patient_id),
    KEY idx_patient_id (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录用户与患者关系';
