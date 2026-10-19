DROP TABLE IF EXISTS duty_info;
DROP TABLE IF EXISTS appointments_info;
DROP TABLE IF EXISTS order_info;
DROP TABLE IF EXISTS appointment_idempotent;
DROP TABLE IF EXISTS patients_info;
DROP TABLE IF EXISTS user_patient_rel;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_user_hospital;
DROP TABLE IF EXISTS hospital_info;

CREATE TABLE duty_info (
    schedule_id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NULL,
    duty_date DATE NULL,
    duty_period VARCHAR(255) NULL,
    appointments_num BIGINT NULL,
    appointments_num_made BIGINT NULL DEFAULT 0,
    registration_fee DECIMAL(10, 2) NULL,
    del_flag VARCHAR(255) NULL DEFAULT '0',
    create_by VARCHAR(255) NULL,
    create_time DATETIME NULL,
    update_by VARCHAR(255) NULL,
    update_time DATETIME NULL,
    remark VARCHAR(255) NULL,
    PRIMARY KEY (schedule_id)
);

CREATE TABLE appointments_info (
    appointments_id BIGINT NOT NULL,
    schedule_id BIGINT NULL,
    doctor_id BIGINT NULL,
    patient_id BIGINT NULL,
    status VARCHAR(255) NULL DEFAULT '0',
    visit_time DATE NULL,
    del_flag VARCHAR(255) NULL DEFAULT '0',
    -- 有效预约唯一占位患者ID，与生产 MySQL 5.7.18 的 STORED 生成列语义一致：
    -- del_flag='0' 且 status IN ('0','1') 时取 patient_id，否则为 NULL。
    -- 只用于唯一性判定，不代表号源计数（duty_info.appointments_num_made 只增不减）。
    -- 生产库使用 MySQL 生成列语法（GENERATED ALWAYS AS ... STORED），
    -- H2 1.4.200 不支持该关键字，此处改用 H2 原生计算列语法表达同一约束。
    active_patient_id BIGINT AS (
        CASE WHEN del_flag = '0' AND status IN ('0', '1')
                  AND schedule_id IS NOT NULL AND patient_id IS NOT NULL
             THEN patient_id
             ELSE NULL END),
    create_by VARCHAR(255) NULL,
    create_time DATETIME NULL,
    update_by VARCHAR(255) NULL,
    update_time DATETIME NULL,
    remark VARCHAR(255) NULL,
    PRIMARY KEY (appointments_id)
);

-- 由database仲裁「同一排班 + 同一患者最多一条有效预约」，唯一索引允许多个 NULL。
-- 不建 (schedule_id, patient_id) 永久唯一索引：取消/退款后需允许重新预约。
CREATE UNIQUE INDEX uk_active_schedule_patient ON appointments_info (schedule_id, active_patient_id);

-- 幂等记录表，对应生产迁移脚本 appointment_phase2.sql 步骤 1
CREATE TABLE appointment_idempotent (
    user_id BIGINT NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    appointments_id BIGINT NULL,
    order_id BIGINT NULL,
    result_status VARCHAR(16) NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (user_id, request_id)
);

CREATE TABLE order_info (
    order_id BIGINT NOT NULL,
    appointments_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    order_amount DECIMAL(10, 2) NULL,
    pay_state VARCHAR(255) NULL DEFAULT '0',
    payment_deadline DATETIME NULL,
    del_flag VARCHAR(255) NULL DEFAULT '0',
    create_by VARCHAR(255) NULL,
    create_time DATETIME NULL,
    update_by VARCHAR(255) NULL,
    update_time DATETIME NULL,
    remark VARCHAR(255) NULL,
    PRIMARY KEY (order_id)
);

CREATE TABLE patients_info (
    patient_id BIGINT NOT NULL AUTO_INCREMENT,
    card_no VARCHAR(255) NULL,
    nick_name VARCHAR(255) NULL,
    sex VARCHAR(255) NULL,
    phone VARCHAR(255) NULL,
    del_flag VARCHAR(255) NULL DEFAULT '0',
    create_by VARCHAR(255) NULL,
    create_time DATETIME NULL,
    update_by VARCHAR(255) NULL,
    update_time DATETIME NULL,
    remark VARCHAR(255) NULL,
    PRIMARY KEY (patient_id)
);

CREATE TABLE user_patient_rel (
    user_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    relation_type VARCHAR(32) NOT NULL DEFAULT 'OWNER',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, patient_id)
);

-- duty_info 查询会关联以下系统表，测试中仅需保证表结构存在
CREATE TABLE sys_user (
    user_id BIGINT NOT NULL,
    del_flag VARCHAR(255) NULL,
    dept_id BIGINT NULL,
    nick_name VARCHAR(255) NULL,
    remark VARCHAR(255) NULL,
    PRIMARY KEY (user_id)
);

CREATE TABLE sys_dept (
    dept_id BIGINT NOT NULL,
    del_flag VARCHAR(255) NULL,
    dept_name VARCHAR(255) NULL,
    PRIMARY KEY (dept_id)
);

CREATE TABLE sys_user_hospital (
    user_id BIGINT NOT NULL,
    hospital_id BIGINT NULL
);

CREATE TABLE hospital_info (
    id BIGINT NOT NULL,
    del_flag VARCHAR(255) NULL,
    hospital_name VARCHAR(255) NULL,
    PRIMARY KEY (id)
);
