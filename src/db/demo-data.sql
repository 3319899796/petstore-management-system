-- ============================================================
-- 演示数据（可选，答辩前执行一次即可让系统"有内容"）
-- 前置：先执行 schema.sql 建库建表，再启动一次后端（生成 admin/staff/owner 三个账号）
-- 执行方式：mysql -u root -p pet_store < demo-data.sql
-- 说明：日期是按 2026 年 11 月设计的，让"即将到期/已逾期"的疫苗记录都能被看到
-- ============================================================

USE pet_store;

-- 清空业务数据，保证脚本可以重复执行（不动 sys_user 和 role）
DELETE FROM notice;
DELETE FROM adoption;
DELETE FROM foster;
DELETE FROM medical;
DELETE FROM vaccine;
DELETE FROM pet;
DELETE FROM owner;
ALTER TABLE owner AUTO_INCREMENT = 1;
ALTER TABLE pet AUTO_INCREMENT = 1;
ALTER TABLE vaccine AUTO_INCREMENT = 1;
ALTER TABLE medical AUTO_INCREMENT = 1;
ALTER TABLE foster AUTO_INCREMENT = 1;
ALTER TABLE adoption AUTO_INCREMENT = 1;
ALTER TABLE notice AUTO_INCREMENT = 1;

-- ------------------------------------------------------------
-- 1. 主人：张伟绑定 owner 账号（登录后只能看到自己的宠物），另外两位没有登录账号
-- ------------------------------------------------------------
INSERT INTO owner (owner_name, phone, user_id) VALUES
('张伟', '13800000001', (SELECT user_id FROM sys_user WHERE username = 'owner')),
('李娜', '13900000002', NULL),
('王强', '13700000003', NULL);

-- ------------------------------------------------------------
-- 2. 宠物档案（一户多宠：张伟 2 只，李娜 2 只，王强 2 只）
-- ------------------------------------------------------------
INSERT INTO pet (pet_name, chip_no, breed, owner_id, photo_path) VALUES
('豆豆', 'CHIP-DOG-0001', '金毛寻回犬', 1, NULL),
('汤圆', 'CHIP-CAT-0002', '英国短毛猫', 1, NULL),
('可乐', 'CHIP-DOG-0003', '柯基', 2, NULL),
('布丁', 'CHIP-CAT-0004', '布偶猫', 2, NULL),
('球球', 'CHIP-DOG-0005', '泰迪', 3, NULL),
('奶糖', 'CHIP-CAT-0006', '中华田园猫', 3, NULL);

-- ------------------------------------------------------------
-- 3. 免疫记录：故意造出"已逾期""7 天内到期""还很远"三种情况
-- ------------------------------------------------------------
INSERT INTO vaccine (pet_id, vaccine_type, inoculate_date, next_due_date, reminded) VALUES
(1, '狂犬疫苗', '2025-11-10', '2026-11-10', 0),   -- 已逾期（2026-11-23 今天）
(1, '犬四联',   '2026-01-05', '2026-11-26', 0),   -- 3 天后到期
(2, '猫三联',   '2026-03-01', '2026-11-28', 0),   -- 5 天后到期
(3, '狂犬疫苗', '2026-06-15', '2027-06-15', 0),   -- 还很久
(4, '猫三联',   '2025-12-01', '2026-12-01', 0),   -- 8 天后到期（不提醒）
(5, '犬四联',   '2026-02-20', '2026-11-20', 1);   -- 已提醒过（reminded=1）

-- ------------------------------------------------------------
-- 4. 就诊记录
-- ------------------------------------------------------------
INSERT INTO medical (pet_id, symptom, diagnosis, visit_date) VALUES
(1, '持续咳嗽、食欲下降', '上呼吸道感染，开具止咳药', '2026-10-12'),
(3, '后腿跛行', '髋关节轻微扭伤，建议静养两周', '2026-11-05'),
(4, '呕吐两次、精神萎靡', '毛球症，喂化毛膏并观察', '2026-11-18');

-- ------------------------------------------------------------
-- 5. 寄养记录：寄养中 / 待审核 / 已结束 各一条
-- ------------------------------------------------------------
INSERT INTO foster (pet_id, start_date, end_date, status, handler_id, create_time) VALUES
(1, '2026-11-20', '2026-12-05', 'APPROVED', (SELECT user_id FROM sys_user WHERE username = 'staff'), '2026-11-19 10:20:00'),
(5, '2026-12-01', '2026-12-10', 'PENDING',  NULL, '2026-11-22 15:30:00'),
(3, '2026-09-01', '2026-09-10', 'FINISHED', (SELECT user_id FROM sys_user WHERE username = 'staff'), '2026-08-28 09:00:00');

-- ------------------------------------------------------------
-- 6. 领养记录：待审核 / 已通过（含回访） / 已驳回
-- ------------------------------------------------------------
INSERT INTO adoption (pet_id, applicant_id, status, apply_time, audit_time, auditor_id, reject_reason, follow_up_time, follow_up_result) VALUES
(6, 2, 'PENDING', '2026-11-21 14:00:00', NULL, NULL, NULL, NULL, NULL),
(4, 3, 'APPROVED', '2026-10-01 09:30:00', '2026-10-03 11:00:00', (SELECT user_id FROM sys_user WHERE username = 'admin'), NULL, '2026-11-01 10:00:00', '已适应新环境，饮食正常，体重增加 0.4kg'),
(2, 3, 'REJECTED', '2026-10-20 16:20:00', '2026-10-22 09:10:00', (SELECT user_id FROM sys_user WHERE username = 'admin'), '申请人已有两宠，饲养条件暂不满足', NULL, NULL);

-- ------------------------------------------------------------
-- 7. 公告
-- ------------------------------------------------------------
INSERT INTO notice (title, content, publisher_id, publish_time) VALUES
('门店寄养区扩建完成通知', '寄养区新增 6 个独立隔间，2026 年 12 月起可接待小型犬与猫类住宿，预约请在系统中提交寄养申请。', (SELECT user_id FROM sys_user WHERE username = 'admin'), '2026-11-15 09:00:00'),
('冬季疫苗集中接种安排', '11 月 25 日至 12 月 5 日期间提供疫苗门诊，请各位主人查看免疫记录中的到期时间，提前预约。', (SELECT user_id FROM sys_user WHERE username = 'admin'), '2026-11-20 10:30:00');

-- ------------------------------------------------------------
-- 8. 兜底绑定：如果本脚本是在"第一次启动后端"之前执行的，
--    上面那些 (SELECT user_id FROM sys_user ...) 会取到 NULL（因为账号还不存在），
--    这里再补一次，保证「owner 登录后能看到自己的宠物」这条演示一定能成立。
-- ------------------------------------------------------------
UPDATE owner SET user_id = (SELECT user_id FROM sys_user WHERE username = 'owner')
 WHERE user_id IS NULL AND owner_name = '张伟';
UPDATE foster SET handler_id = (SELECT user_id FROM sys_user WHERE username = 'staff')
 WHERE handler_id IS NULL AND status IN ('APPROVED', 'FINISHED');
UPDATE adoption SET auditor_id = (SELECT user_id FROM sys_user WHERE username = 'admin')
 WHERE auditor_id IS NULL AND status <> 'PENDING';
UPDATE notice SET publisher_id = (SELECT user_id FROM sys_user WHERE username = 'admin')
 WHERE publisher_id IS NULL;

-- ------------------------------------------------------------
-- 9. 一次性触发提醒（也可以登录后在"公告管理"里点"立即执行提醒扫描"）
-- 提醒是后端 Spring Task 生成的，所以执行完本脚本后启动后端、点一次按钮即可看到站内提醒
-- ------------------------------------------------------------
