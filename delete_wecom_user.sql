-- ============================================
-- 删除企业微信用户 wecom_HanXiaoLong 及关联数据
-- 已于 2026-05-21 在云服务器成功执行
-- 执行方式：
--   ssh root@101.32.240.27
--   PGPASSWORD='7sK$9pR@2xL#8tGz' psql -h 10.0.24.16 -p 5432 -U admin -d apartment-mng2
-- ============================================

-- Step 1: 查找目标用户（已确认 id=10）
-- SELECT id, username, real_name, phone, email, wecom_id, source, status FROM sys_user WHERE username = 'wecom_HanXiaoLong';

-- Step 2: 查看关联数据数量
-- SELECT 'booked_orders' AS type, count(*) FROM room_order WHERE book_user_id = 10;
-- SELECT 'occupies' AS type, count(*) FROM room_occupy WHERE occupant_user_id = 10;
-- SELECT 'order_logs' AS type, count(*) FROM order_log WHERE operator_id = 10;
-- SELECT 'notifications' AS type, count(*) FROM notification_record WHERE recipient_user_id = 10;

-- ========== 删除操作（按 FK 依赖顺序） ==========

-- Step 3: 删除订单子表（order_product_detail, order_fee）
DELETE FROM order_product_detail WHERE order_id IN (SELECT id FROM room_order WHERE book_user_id = :USER_ID);
DELETE FROM order_fee WHERE order_id IN (SELECT id FROM room_order WHERE book_user_id = :USER_ID);

-- Step 4: 删除该订单的日志（必须在删订单之前，因为 order_log.order_id FK 引用 room_order）
DELETE FROM order_log WHERE order_id IN (SELECT id FROM room_order WHERE book_user_id = :USER_ID);
DELETE FROM order_log WHERE operator_id = :USER_ID;

-- Step 5: 删除通知记录（必须在 room_occupy 之前，因为 notification_record.occupy_id FK 引用 room_occupy）
DELETE FROM notification_record WHERE recipient_user_id = :USER_ID;
DELETE FROM notification_record WHERE occupy_id IN (SELECT id FROM room_occupy WHERE occupant_user_id = :USER_ID);
DELETE FROM notification_record WHERE occupy_id IN (SELECT id FROM room_occupy WHERE order_id IN (SELECT id FROM room_order WHERE book_user_id = :USER_ID));

-- Step 6: 删除入住记录
DELETE FROM room_occupy WHERE occupant_user_id = :USER_ID;
DELETE FROM room_occupy WHERE order_id IN (SELECT id FROM room_order WHERE book_user_id = :USER_ID);

-- Step 7: 删除该用户的订单
DELETE FROM room_order WHERE book_user_id = :USER_ID;

-- Step 8: 删除用户角色关联
DELETE FROM sys_user_role WHERE user_id = :USER_ID;

-- Step 9: 删除用户
DELETE FROM sys_user WHERE id = :USER_ID;