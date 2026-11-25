-- ============================================================
-- 基于 SpringBoot 的宠物店宠物信息管理系统  数据库脚本
-- 数据库：MySQL 8.0   字符集：utf8mb4   引擎：InnoDB
-- 依据：开题报告（五）数据库设计，9 张表，第三范式
-- 说明：user 是 MySQL 保留关键字，用户表实际命名为 sys_user
-- 执行方式：mysql -u root -p < schema.sql
-- ============================================================

DROP DATABASE IF EXISTS pet_store;
CREATE DATABASE pet_store DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE pet_store;

-- ------------------------------------------------------------
-- 1. 角色表 role
-- ------------------------------------------------------------
CREATE TABLE role (
    role_id     INT          NOT NULL AUTO_INCREMENT COMMENT '角色主键',
    role_name   VARCHAR(20)  NOT NULL COMMENT '角色名称：宠物店管理员/工作人员/宠物主人',
    permission  TEXT         NULL     COMMENT '权限码，逗号分隔；* 表示全部权限',
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ------------------------------------------------------------
-- 2. 用户表 sys_user
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    user_id     INT          NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    username    VARCHAR(30)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储，不存明文）',
    role_id     INT          NOT NULL COMMENT '外键，关联角色表',
    phone       VARCHAR(20)  NULL     COMMENT '手机号（展示时脱敏）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username),
    KEY idx_user_role (role_id),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（user 为保留关键字，改名 sys_user）';

-- ------------------------------------------------------------
-- 3. 主人表 owner
-- ------------------------------------------------------------
CREATE TABLE owner (
    owner_id    INT          NOT NULL AUTO_INCREMENT COMMENT '主人主键',
    owner_name  VARCHAR(30)  NOT NULL COMMENT '主人姓名',
    phone       VARCHAR(20)  NULL     COMMENT '手机号（列表脱敏：138****8000）',
    user_id     INT          NULL     COMMENT '外键，关联用户表（主人登录账号，可空）',
    PRIMARY KEY (owner_id),
    UNIQUE KEY uk_owner_user (user_id),
    CONSTRAINT fk_owner_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物主人表';

-- ------------------------------------------------------------
-- 4. 宠物档案表 pet
-- ------------------------------------------------------------
CREATE TABLE pet (
    pet_id      INT          NOT NULL AUTO_INCREMENT COMMENT '宠物主键',
    pet_name    VARCHAR(30)  NOT NULL COMMENT '宠物名称',
    chip_no     VARCHAR(30)  NOT NULL COMMENT '芯片号（唯一索引）',
    breed       VARCHAR(30)  NULL     COMMENT '品种',
    owner_id    INT          NOT NULL COMMENT '外键，关联主人表（一户多宠）',
    photo_path  VARCHAR(255) NULL     COMMENT '照片访问路径（存路径不存文件本体）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
    PRIMARY KEY (pet_id),
    UNIQUE KEY uk_pet_chip (chip_no),
    KEY idx_pet_owner (owner_id),
    CONSTRAINT fk_pet_owner FOREIGN KEY (owner_id) REFERENCES owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';

-- ------------------------------------------------------------
-- 5. 免疫记录表 vaccine
-- ------------------------------------------------------------
CREATE TABLE vaccine (
    vaccine_id      INT      NOT NULL AUTO_INCREMENT COMMENT '免疫记录主键',
    pet_id          INT      NOT NULL COMMENT '外键，关联宠物档案表（一宠多记录）',
    vaccine_type    VARCHAR(30) NOT NULL COMMENT '疫苗种类',
    inoculate_date  DATE     NOT NULL COMMENT '接种日期',
    next_due_date   DATE     NULL     COMMENT '下次到期日（按免疫周期自动计算）',
    reminded        TINYINT  NOT NULL DEFAULT 0 COMMENT '提醒去重标记：0 未提醒，1 已提醒',
    PRIMARY KEY (vaccine_id),
    KEY idx_vaccine_pet (pet_id),
    KEY idx_vaccine_due (next_due_date, reminded),
    CONSTRAINT fk_vaccine_pet FOREIGN KEY (pet_id) REFERENCES pet (pet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='免疫记录表';

-- ------------------------------------------------------------
-- 6. 就诊记录表 medical
-- ------------------------------------------------------------
CREATE TABLE medical (
    medical_id  INT          NOT NULL AUTO_INCREMENT COMMENT '就诊记录主键',
    pet_id      INT          NOT NULL COMMENT '外键，关联宠物档案表',
    symptom     VARCHAR(255) NULL     COMMENT '症状',
    diagnosis   VARCHAR(255) NULL     COMMENT '诊断结果',
    visit_date  DATE         NOT NULL COMMENT '就诊日期',
    PRIMARY KEY (medical_id),
    KEY idx_medical_pet (pet_id),
    CONSTRAINT fk_medical_pet FOREIGN KEY (pet_id) REFERENCES pet (pet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就诊记录表';

-- ------------------------------------------------------------
-- 7. 领养申请表 adoption（回访记录以扩展字段存放，不单独建表）
-- ------------------------------------------------------------
CREATE TABLE adoption (
    adoption_id     INT          NOT NULL AUTO_INCREMENT COMMENT '领养申请主键',
    pet_id          INT          NOT NULL COMMENT '外键，关联宠物档案表',
    applicant_id    INT          NOT NULL COMMENT '外键，关联主人表（申请人）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待审核/APPROVED 已通过/REJECTED 已驳回',
    apply_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    audit_time      DATETIME     NULL     COMMENT '审核时间',
    auditor_id      INT          NULL     COMMENT '审核人（用户主键）',
    reject_reason   VARCHAR(255) NULL     COMMENT '驳回原因',
    follow_up_time  DATETIME     NULL     COMMENT '回访时间（扩展字段）',
    follow_up_result VARCHAR(255) NULL    COMMENT '回访结果（扩展字段）',
    PRIMARY KEY (adoption_id),
    KEY idx_adoption_pet (pet_id),
    KEY idx_adoption_applicant (applicant_id),
    CONSTRAINT fk_adoption_pet FOREIGN KEY (pet_id) REFERENCES pet (pet_id),
    CONSTRAINT fk_adoption_applicant FOREIGN KEY (applicant_id) REFERENCES owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领养申请表（含回访扩展字段）';

-- ------------------------------------------------------------
-- 8. 寄养记录表 foster
-- ------------------------------------------------------------
CREATE TABLE foster (
    foster_id   INT         NOT NULL AUTO_INCREMENT COMMENT '寄养记录主键',
    pet_id      INT         NOT NULL COMMENT '外键，关联宠物档案表',
    start_date  DATE        NOT NULL COMMENT '寄养开始日期',
    end_date    DATE        NOT NULL COMMENT '寄养结束日期',
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待审核/APPROVED 寄养中/FINISHED 已结束/REJECTED 已驳回',
    handler_id  INT         NULL     COMMENT '处理人（用户主键）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (foster_id),
    KEY idx_foster_pet (pet_id),
    CONSTRAINT fk_foster_pet FOREIGN KEY (pet_id) REFERENCES pet (pet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寄养记录表';

-- ------------------------------------------------------------
-- 9. 公告表 notice
-- ------------------------------------------------------------
CREATE TABLE notice (
    notice_id    INT          NOT NULL AUTO_INCREMENT COMMENT '公告主键',
    title        VARCHAR(100) NOT NULL COMMENT '标题',
    content      TEXT         NULL     COMMENT '正文',
    publisher_id INT          NULL     COMMENT '发布人（用户主键）',
    publish_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (notice_id),
    KEY idx_notice_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ============================================================
-- 初始化数据：三个角色（权限码与后端 @RequirePerm 一一对应）
-- 演示账号由后端 DataInitializer 启动时用 BCrypt 加密写入，脚本里不放明文密码哈希
-- ============================================================
INSERT INTO role (role_id, role_name, permission) VALUES
(1, '宠物店管理员', '*'),
(2, '工作人员', 'pet:list,pet:view,pet:edit,owner:list,vaccine:list,vaccine:add,vaccine:edit,medical:list,medical:add,medical:edit,foster:list,foster:audit,adoption:list,adoption:audit,adoption:followup,notice:list,stat:view'),
(3, '宠物主人', 'pet:list,pet:view,pet:add,pet:edit,vaccine:list,medical:list,foster:list,foster:add,adoption:list,adoption:add,notice:list');
