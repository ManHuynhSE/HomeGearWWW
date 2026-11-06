-- Run after the application has created the MariaDB tables.
-- Safe to rerun: categories are upserted and products are added only when absent.

INSERT INTO categories (name)
VALUES
    ('Điện tử'),
    ('Máy lạnh'),
    ('Nhà bếp'),
    ('Thiết bị thông minh')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO products
    (name, price, original_price, promotion_starts_at, promotion_ends_at, stock, description, category_id)
SELECT
    seed.name,
    seed.price,
    seed.original_price,
    seed.promotion_starts_at,
    seed.promotion_ends_at,
    seed.stock,
    seed.description,
    category.id
FROM (
    SELECT
        'Nồi chiên không dầu 5L' AS name,
        1890000.00 AS price,
        2490000.00 AS original_price,
        DATE_SUB(NOW(), INTERVAL 1 HOUR) AS promotion_starts_at,
        DATE_ADD(NOW(), INTERVAL 7 DAY) AS promotion_ends_at,
        18 AS stock,
        'Nồi chiên dung tích 5 lít, phù hợp cho bữa ăn gia đình.' AS description,
        'Nhà bếp' AS category_name
    UNION ALL SELECT
        'Máy xay sinh tố đa năng', 790000.00, NULL, NULL, NULL, 25,
        'Máy xay đa năng với nhiều tốc độ, tiện dụng cho căn bếp.', 'Nhà bếp'
    UNION ALL SELECT
        'Máy lạnh Inverter 1 HP', 7490000.00, 8990000.00,
        DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 12,
        'Máy lạnh Inverter tiết kiệm điện, phù hợp phòng nhỏ.', 'Máy lạnh'
    UNION ALL SELECT
        'Smart TV 4K 55 inch', 8990000.00, NULL, NULL, NULL, 10,
        'Smart TV độ phân giải 4K, màn hình 55 inch.', 'Điện tử'
    UNION ALL SELECT
        'Máy lọc không khí', 3290000.00, NULL, NULL, NULL, 16,
        'Máy lọc không khí cho không gian sinh hoạt gia đình.', 'Điện tử'
    UNION ALL SELECT
        'Ổ cắm thông minh Wi-Fi', 350000.00, NULL, NULL, NULL, 50,
        'Điều khiển thiết bị điện từ xa qua kết nối Wi-Fi.', 'Thiết bị thông minh'
    UNION ALL SELECT
        'Đèn thông minh RGB', 490000.00, NULL, NULL, NULL, 30,
        'Đèn thông minh có thể điều chỉnh màu sắc và độ sáng.', 'Thiết bị thông minh'
) AS seed
JOIN categories AS category ON category.name = seed.category_name
WHERE NOT EXISTS (
    SELECT 1
    FROM products AS existing
    WHERE existing.name = seed.name
      AND existing.category_id = category.id
);
