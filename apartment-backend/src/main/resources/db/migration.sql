-- Migration: i18n, key box, anti-duplicate, field cleanup
-- Execute manually against the database

-- #2: User locale preference
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS locale VARCHAR(5) DEFAULT 'zh';

-- #5: Room key sending - add order-level key box fields
ALTER TABLE room_order ADD COLUMN IF NOT EXISTS key_box_no VARCHAR(50);
ALTER TABLE room_order ADD COLUMN IF NOT EXISTS box_password VARCHAR(50);

-- #5: Room key sending - add notification fields
ALTER TABLE notification_record ADD COLUMN IF NOT EXISTS key_box_no VARCHAR(50);
ALTER TABLE notification_record ADD COLUMN IF NOT EXISTS box_password VARCHAR(50);
ALTER TABLE notification_record ADD COLUMN IF NOT EXISTS locale VARCHAR(5);

-- #7: Notification message type
ALTER TABLE notification_record ADD COLUMN IF NOT EXISTS message_type VARCHAR(30);

-- #5: Remove roomCardNo/doorCode from notification_record (replaced by keyBoxNo/boxPassword)
ALTER TABLE notification_record DROP COLUMN IF EXISTS room_card_no;
ALTER TABLE notification_record DROP COLUMN IF EXISTS door_code;

-- #5: Remove roomCardNo/doorCode from room_occupy (fully deprecated)
ALTER TABLE room_occupy DROP COLUMN IF EXISTS room_card_no;
ALTER TABLE room_occupy DROP COLUMN IF EXISTS door_code;

-- #4: Anti-duplicate - unique partial index for one cooling-off order per user
CREATE UNIQUE INDEX IF NOT EXISTS one_cooling_off_per_user ON room_order (book_user_id) WHERE status = 0;

-- #6: External group booking - order-level fields
ALTER TABLE room_order ADD COLUMN IF NOT EXISTS group_name VARCHAR(100);
ALTER TABLE room_order ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100);
ALTER TABLE room_order ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30);
ALTER TABLE room_order ADD COLUMN IF NOT EXISTS activity_code VARCHAR(50);
ALTER TABLE room_order ADD COLUMN IF NOT EXISTS project_code VARCHAR(50);

-- #6: External group booking - occupy-level occupant name (group name for group orders)
ALTER TABLE room_occupy ADD COLUMN IF NOT EXISTS occupant_name VARCHAR(100);

-- #7: RoomOccupy status expansion: 0=Pending(待入住), 1=Checked-in(已入住), 2=Checked-out(已退房), 3=Canceled(取消)
-- Old values: 0=Current(当前), 1=Finish(完成)
-- Two-step shift to avoid value collision

-- Step 1: Shift old values out of the way
UPDATE room_occupy SET status = 10 WHERE status = 0;
UPDATE room_occupy SET status = 11 WHERE status = 1;

-- Step 2: Map old Current(10) under In orders -> Checked-in(1)
UPDATE room_occupy ro SET status = 1
  WHERE ro.status = 10
  AND EXISTS (SELECT 1 FROM room_order o WHERE o.id = ro.order_id AND o.status = 2);

-- Step 3: Map old Current(10) under Canceled orders -> Canceled(3)
UPDATE room_occupy ro SET status = 3
  WHERE ro.status = 10
  AND EXISTS (SELECT 1 FROM room_order o WHERE o.id = ro.order_id AND o.status = 4);

-- Step 4: Map old Current(10) under Checked-out orders -> Checked-out(2)
UPDATE room_occupy ro SET status = 2
  WHERE ro.status = 10
  AND EXISTS (SELECT 1 FROM room_order o WHERE o.id = ro.order_id AND o.status = 3);

-- Step 5: Remaining old Current(10) -> Pending(0)
UPDATE room_occupy SET status = 0 WHERE status = 10;

-- Step 6: Map old Finish(11) -> Checked-out(2)
UPDATE room_occupy SET status = 2 WHERE status = 11;
