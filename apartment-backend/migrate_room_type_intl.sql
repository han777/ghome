-- ============================================================
-- 迁移脚本：将 room_type_name_intl 子表数据合并到 room_type.name_intl_json
-- 在 Hibernate ddl-auto: update 生成新列后执行此脚本
-- ============================================================

-- Step 1: 为 room_type 表添加 name_intl_json 列（如果 Hibernate 未自动添加）
ALTER TABLE room_type ADD COLUMN IF NOT EXISTS name_intl_json jsonb;

-- Step 2: 将子表数据聚合成 JSON 对象写入新列
UPDATE room_type rt
SET name_intl_json = (
    SELECT jsonb_object_agg(lang, name)
    FROM room_type_name_intl
    WHERE room_type_id = rt.id
)
WHERE EXISTS (
    SELECT 1 FROM room_type_name_intl WHERE room_type_id = rt.id
);

-- Step 3: 验证结果（查看转换后数据）
SELECT id, type_code, name_intl_json FROM room_type;

-- Step 4: 确认数据正确后，可删除旧子表（可选，保留以防回滚）
-- DROP TABLE IF EXISTS room_type_name_intl;
