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

-- #6: External group booking - occupy-level occupant name (group name for group orders)
ALTER TABLE room_occupy ADD COLUMN IF NOT EXISTS occupant_name VARCHAR(100);
