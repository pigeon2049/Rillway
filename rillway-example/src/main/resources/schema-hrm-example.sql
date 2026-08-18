-- =========================================================================
-- Rillway HRM 考勤审批与组织架构示例脱敏 SQL 脚本 (H2 / MySQL 兼容)
-- =========================================================================

-- 1. HRM 考勤请假单表
CREATE TABLE IF NOT EXISTS hrm_attendance_leave (
    id BIGINT NOT NULL PRIMARY KEY,
    employee_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    "day" DECIMAL(5, 1) NOT NULL,
    reason VARCHAR(500),
    remark VARCHAR(500),
    approval_status INT DEFAULT 1,
    creator VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1
);

-- 2. 系统用户/员工表 (脱敏)
CREATE TABLE IF NOT EXISTS system_users (
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    dept_id BIGINT,
    direct_leader_id BIGINT,
    mobile VARCHAR(32),
    status INT DEFAULT 0,
    deleted INT DEFAULT 0
);

-- 3. 系统企业部门表 (脱敏)
CREATE TABLE IF NOT EXISTS system_dept (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    leader_user_id BIGINT,
    parent_id BIGINT DEFAULT 0,
    status INT DEFAULT 0,
    deleted INT DEFAULT 0
);

-- 4. 系统角色表 (脱敏)
CREATE TABLE IF NOT EXISTS system_role (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status INT DEFAULT 0,
    deleted INT DEFAULT 0
);

-- 5. 系统岗位表 (脱敏)
CREATE TABLE IF NOT EXISTS system_post (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status INT DEFAULT 0,
    deleted INT DEFAULT 0
);

-- 6. 用户-角色关联表 (脱敏)
CREATE TABLE IF NOT EXISTS system_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

-- =========================================================================
-- 初始化脱敏后的演示数据
-- =========================================================================

-- 部门数据
MERGE INTO system_dept KEY(id) VALUES (100, '总经办', 1, 0, 0, 0);
MERGE INTO system_dept KEY(id) VALUES (101, '研发部', 10, 100, 0, 0);
MERGE INTO system_dept KEY(id) VALUES (102, '市场营销部', 20, 100, 0, 0);
MERGE INTO system_dept KEY(id) VALUES (103, '人力资源部', 30, 100, 0, 0);

-- 用户数据 (员工 Alice, 主管 Bob, 市场总监 Charlie, HR总监 Helen, 总经理 David)
MERGE INTO system_users KEY(id) VALUES (1, 'david_gm', '大卫 (总经理)', 100, NULL, '13800000001', 0, 0);
MERGE INTO system_users KEY(id) VALUES (10, 'bob_rd_mgr', '鲍勃 (研发主管)', 101, 1, '13800000010', 0, 0);
MERGE INTO system_users KEY(id) VALUES (20, 'charlie_mkt_dir', '查理 (市场总监)', 102, 1, '13800000020', 0, 0);
MERGE INTO system_users KEY(id) VALUES (30, 'helen_hr_dir', '海伦 (人事总监)', 103, 1, '13800000030', 0, 0);
MERGE INTO system_users KEY(id) VALUES (100, 'alice_emp', '爱丽丝 (研发工程师)', 101, 10, '13800000100', 0, 0);

-- 角色数据
MERGE INTO system_role KEY(id) VALUES (1, '总经理', 'ROLE_GENERAL_MANAGER', 0, 0);
MERGE INTO system_role KEY(id) VALUES (2, '市场总监', 'ROLE_MARKETING_DIRECTOR', 0, 0);
MERGE INTO system_role KEY(id) VALUES (3, '人事总监', 'ROLE_HR_DIRECTOR', 0, 0);

-- 岗位数据
MERGE INTO system_post KEY(id) VALUES (1, '高级研发工程师', 'POST_SR_DEV', 0, 0);
MERGE INTO system_post KEY(id) VALUES (2, '部门经理', 'POST_DEPT_MGR', 0, 0);
