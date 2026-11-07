-- Run after the application has created/updated the MariaDB tables.
-- Safe to rerun: categories are upserted, seeded products are added only once,
-- and existing product images are filled only when image_url is empty.
-- Product photos are hosted by Wikimedia Commons; creator/license credits link to each file page.

INSERT INTO categories (name)
VALUES
    ('Điện tử'),
    ('Máy lạnh'),
    ('Nhà bếp'),
    ('Thiết bị thông minh'),
    ('Vệ sinh nhà cửa'),
    ('Giặt ủi'),
    ('Chiếu sáng')
ON DUPLICATE KEY UPDATE name = VALUES(name);

UPDATE products AS existing
JOIN categories AS category ON category.id = existing.category_id
JOIN (
    SELECT 'Nồi chiên không dầu 5L' AS name, 'Nhà bếp' AS category_name,
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d3/Air_Fryer_2020.jpg/500px-Air_Fryer_2020.jpg' AS image_url,
        'I am R. · CC BY 2.0' AS image_attribution,
        'https://commons.wikimedia.org/wiki/File:Air_Fryer_2020.jpg' AS image_source_url
    UNION ALL SELECT 'Máy xay sinh tố đa năng', 'Nhà bếp',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a3/Pink_portable_blender.jpg/500px-Pink_portable_blender.jpg',
        'Asecretfanfare · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:Pink_portable_blender.jpg'
    UNION ALL SELECT 'Máy lạnh Inverter 1 HP', 'Máy lạnh',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2d/DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg/500px-DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg',
        'Dinkun Chen · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg'
    UNION ALL SELECT 'Smart TV 4K 55 inch', 'Điện tử',
        'https://upload.wikimedia.org/wikipedia/commons/8/8f/A_Flat_Screen_Television.jpg',
        'Kid Keen 47 · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:A_Flat_Screen_Television.jpg'
    UNION ALL SELECT 'Máy lọc không khí', 'Điện tử',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f5/Air_Purifier_%28Levoit_LV-H133%29_%2849318569587%29.jpg/500px-Air_Purifier_%28Levoit_LV-H133%29_%2849318569587%29.jpg',
        'Home Air Quality Guides · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:Air_Purifier_(Levoit_LV-H133)_(49318569587).jpg'
    UNION ALL SELECT 'Ổ cắm thông minh Wi-Fi', 'Thiết bị thông minh',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/10/Wemo_Mini_Smart_Plug_%283750%29.jpg/500px-Wemo_Mini_Smart_Plug_%283750%29.jpg',
        'Gregory Varnum · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:Wemo_Mini_Smart_Plug_(3750).jpg'
) AS seed ON seed.name = existing.name AND seed.category_name = category.name
SET existing.image_url = seed.image_url,
    existing.image_attribution = seed.image_attribution,
    existing.image_source_url = seed.image_source_url
WHERE existing.image_url IS NULL OR existing.image_url = '';

INSERT INTO products
    (name, price, original_price, promotion_starts_at, promotion_ends_at, stock,
     description, image_url, image_attribution, image_source_url, category_id)
SELECT
    seed.name,
    seed.price,
    seed.original_price,
    seed.promotion_starts_at,
    seed.promotion_ends_at,
    seed.stock,
    seed.description,
    seed.image_url,
    seed.image_attribution,
    seed.image_source_url,
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
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d3/Air_Fryer_2020.jpg/500px-Air_Fryer_2020.jpg' AS image_url,
        'I am R. · CC BY 2.0' AS image_attribution,
        'https://commons.wikimedia.org/wiki/File:Air_Fryer_2020.jpg' AS image_source_url,
        'Nhà bếp' AS category_name
    UNION ALL SELECT
        'Máy xay sinh tố đa năng', 790000.00, NULL, NULL, NULL, 25,
        'Máy xay đa năng với nhiều tốc độ, tiện dụng cho căn bếp.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a3/Pink_portable_blender.jpg/500px-Pink_portable_blender.jpg',
        'Asecretfanfare · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:Pink_portable_blender.jpg',
        'Nhà bếp'
    UNION ALL SELECT
        'Máy lạnh Inverter 1 HP', 7490000.00, 8990000.00,
        DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), 12,
        'Máy lạnh Inverter tiết kiệm điện, phù hợp phòng nhỏ.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2d/DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg/500px-DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg',
        'Dinkun Chen · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg',
        'Máy lạnh'
    UNION ALL SELECT
        'Smart TV 4K 55 inch', 8990000.00, NULL, NULL, NULL, 10,
        'Smart TV độ phân giải 4K, màn hình 55 inch.',
        'https://upload.wikimedia.org/wikipedia/commons/8/8f/A_Flat_Screen_Television.jpg',
        'Kid Keen 47 · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:A_Flat_Screen_Television.jpg',
        'Điện tử'
    UNION ALL SELECT
        'Máy lọc không khí', 3290000.00, NULL, NULL, NULL, 16,
        'Máy lọc không khí cho không gian sinh hoạt gia đình.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f5/Air_Purifier_%28Levoit_LV-H133%29_%2849318569587%29.jpg/500px-Air_Purifier_%28Levoit_LV-H133%29_%2849318569587%29.jpg',
        'Home Air Quality Guides · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:Air_Purifier_(Levoit_LV-H133)_(49318569587).jpg',
        'Điện tử'
    UNION ALL SELECT
        'Ổ cắm thông minh Wi-Fi', 350000.00, NULL, NULL, NULL, 50,
        'Điều khiển thiết bị điện từ xa qua kết nối Wi-Fi.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/10/Wemo_Mini_Smart_Plug_%283750%29.jpg/500px-Wemo_Mini_Smart_Plug_%283750%29.jpg',
        'Gregory Varnum · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:Wemo_Mini_Smart_Plug_(3750).jpg',
        'Thiết bị thông minh'
    UNION ALL SELECT
        'Đèn thông minh RGB', 490000.00, NULL, NULL, NULL, 30,
        'Đèn thông minh có thể điều chỉnh màu sắc và độ sáng.',
        NULL, NULL, NULL,
        'Thiết bị thông minh'
    UNION ALL SELECT
        'Máy pha cà phê chân không', 1450000.00, NULL, NULL, NULL, 9,
        'Máy pha cà phê kiểu chân không, ảnh minh họa thiết bị pha cà phê.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/0/03/Vacuum-coffee-maker-1.jpg/500px-Vacuum-coffee-maker-1.jpg',
        'Evelyn-rose · CC0',
        'https://commons.wikimedia.org/wiki/File:Vacuum-coffee-maker-1.jpg',
        'Nhà bếp'
    UNION ALL SELECT
        'Ấm siêu tốc Bosch 1.7L', 890000.00, NULL, NULL, NULL, 21,
        'Ấm siêu tốc dung tích 1,7 lít, tự ngắt khi nước sôi.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3e/2023_Czajnik_elektryczny_Bosch_%281%29.jpg/500px-2023_Czajnik_elektryczny_Bosch_%281%29.jpg',
        'Jacek Halicki · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:2023_Czajnik_elektryczny_Bosch_(1).jpg',
        'Nhà bếp'
    UNION ALL SELECT
        'Lò vi sóng điện tử 20L', 1890000.00, NULL, NULL, NULL, 11,
        'Lò vi sóng để bàn, thiết kế gọn cho căn bếp gia đình.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/3/3f/Kitchenaid_combo_microwave_oven_vent.jpg/500px-Kitchenaid_combo_microwave_oven_vent.jpg',
        'Jdoniach · CC0',
        'https://commons.wikimedia.org/wiki/File:Kitchenaid_combo_microwave_oven_vent.jpg',
        'Nhà bếp'
    UNION ALL SELECT
        'Lò nướng để bàn 12L', 1190000.00, NULL, NULL, NULL, 14,
        'Lò nướng để bàn nhỏ gọn cho món nướng và làm nóng thức ăn.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a8/Hamilton_Beach_Toaster_Oven_Model_31300_-_Door_closed.jpg/500px-Hamilton_Beach_Toaster_Oven_Model_31300_-_Door_closed.jpg',
        'Infrogmation · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:Hamilton_Beach_Toaster_Oven_Model_31300_-_Door_closed.jpg',
        'Nhà bếp'
    UNION ALL SELECT
        'Nồi cơm điện 1.8L', 990000.00, NULL, NULL, NULL, 24,
        'Nồi cơm điện dung tích 1,8 lít, phục vụ gia đình nhiều thành viên.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d3/Air_Fryer_2020.jpg/500px-Air_Fryer_2020.jpg',
        'I am R. · CC BY 2.0',
        'https://commons.wikimedia.org/wiki/File:Air_Fryer_2020.jpg',
        'Nhà bếp'
    UNION ALL SELECT
        'Máy xay cầm tay đa năng', 650000.00, NULL, NULL, NULL, 17,
        'Máy xay cầm tay tiện lợi cho sinh tố và nguyên liệu mềm.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a3/Pink_portable_blender.jpg/500px-Pink_portable_blender.jpg',
        'Asecretfanfare · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:Pink_portable_blender.jpg',
        'Nhà bếp'
    UNION ALL SELECT
        'Máy lạnh Inverter 1.5 HP', 9990000.00, 11990000.00,
        DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 5 DAY), 8,
        'Máy lạnh Inverter công suất 1,5 HP cho phòng ngủ và phòng khách vừa.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/2/2d/DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg/500px-DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg',
        'Dinkun Chen · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT_RSQ200BAY_SPEC_TAG.jpg',
        'Máy lạnh'
    UNION ALL SELECT
        'Máy lạnh Inverter 2 HP', 13990000.00, NULL, NULL, NULL, 6,
        'Máy lạnh Inverter công suất 2 HP cho không gian rộng.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/0/05/LG_AIR_CONDITIONER_OUTDOOR_UNIT%2C_Panasonic_AIR_CONDITIONER_OUTDOOR_UNIT%2C_National_%28Panasonic%29_AIR_CONDITIONER_OUTDOOR_UNIT_%26_DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT.jpg/500px-thumbnail.jpg',
        'Dinkun Chen · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:LG_AIR_CONDITIONER_OUTDOOR_UNIT,_Panasonic_AIR_CONDITIONER_OUTDOOR_UNIT,_National_(Panasonic)_AIR_CONDITIONER_OUTDOOR_UNIT_%26_DAIKIN_AIR_CONDITIONER_OUTDOOR_UNIT.jpg',
        'Máy lạnh'
    UNION ALL SELECT
        'Quạt bàn 3 tốc độ', 450000.00, NULL, NULL, NULL, 32,
        'Quạt bàn nhỏ gọn với các mức gió phù hợp nhu cầu sử dụng hằng ngày.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/3/37/Fan_with_Tesla_induction_motor%2C_1892-1896_-_Museum_of_Science_and_Industry_%28Chicago%29_-_DSC06561.JPG/500px-Fan_with_Tesla_induction_motor%2C_1892-1896_-_Museum_of_Science_and_Industry_%28Chicago%29_-_DSC06561.JPG',
        'Daderot · CC0',
        'https://commons.wikimedia.org/wiki/File:Fan_with_Tesla_induction_motor,_1892-1896_-_Museum_of_Science_and_Industry_(Chicago)_-_DSC06561.JPG',
        'Máy lạnh'
    UNION ALL SELECT
        'Smart TV 4K 43 inch', 6990000.00, NULL, NULL, NULL, 13,
        'Smart TV 4K kích thước 43 inch cho phòng ngủ hoặc căn hộ nhỏ.',
        'https://upload.wikimedia.org/wikipedia/commons/8/8f/A_Flat_Screen_Television.jpg',
        'Kid Keen 47 · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:A_Flat_Screen_Television.jpg',
        'Điện tử'
    UNION ALL SELECT
        'Smart TV 4K 65 inch', 14990000.00, NULL, NULL, NULL, 5,
        'Smart TV màn hình lớn, độ phân giải 4K cho không gian giải trí gia đình.',
        'https://upload.wikimedia.org/wikipedia/commons/8/8f/A_Flat_Screen_Television.jpg',
        'Kid Keen 47 · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:A_Flat_Screen_Television.jpg',
        'Điện tử'
    UNION ALL SELECT
        'Router Wi-Fi băng tần kép', 890000.00, NULL, NULL, NULL, 18,
        'Router Wi-Fi cho kết nối mạng gia đình và nhiều thiết bị cùng lúc.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b9/TP-Link_TL-WR740N_router_HS1_transparent.png/500px-TP-Link_TL-WR740N_router_HS1_transparent.png',
        'Hayden Schiff · CC BY 4.0',
        'https://commons.wikimedia.org/wiki/File:TP-Link_TL-WR740N_router_HS1_transparent.png',
        'Điện tử'
    UNION ALL SELECT
        'Router Wi-Fi 6 gia đình', 1690000.00, 1990000.00,
        DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 6 DAY), 15,
        'Router Wi-Fi thế hệ mới, hỗ trợ nhiều thiết bị trong gia đình.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5f/TP-Link_TL-WR740N_router_HS2.jpg/500px-TP-Link_TL-WR740N_router_HS2.jpg',
        'Hayden Schiff · CC BY 4.0',
        'https://commons.wikimedia.org/wiki/File:TP-Link_TL-WR740N_router_HS2.jpg',
        'Thiết bị thông minh'
    UNION ALL SELECT
        'Máy lọc không khí HEPA', 4290000.00, NULL, NULL, NULL, 7,
        'Máy lọc không khí với bộ lọc HEPA, phù hợp không gian sinh hoạt gia đình.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f5/Air_Purifier_%28Levoit_LV-H133%29_%2849318569587%29.jpg/500px-Air_Purifier_%28Levoit_LV-H133%29_%2849318569587%29.jpg',
        'Home Air Quality Guides · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:Air_Purifier_(Levoit_LV-H133)_(49318569587).jpg',
        'Vệ sinh nhà cửa'
    UNION ALL SELECT
        'Robot hút bụi tự động', 4590000.00, NULL, NULL, NULL, 9,
        'Robot hút bụi tự động hỗ trợ làm sạch sàn nhà.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4f/Robot_Vacuum_-_Charging_%2850841204983%29.jpg/500px-Robot_Vacuum_-_Charging_%2850841204983%29.jpg',
        'ajay_suresh · CC BY 2.0',
        'https://commons.wikimedia.org/wiki/File:Robot_Vacuum_-_Charging_(50841204983).jpg',
        'Vệ sinh nhà cửa'
    UNION ALL SELECT
        'Robot hút bụi lau nhà', 6290000.00, 7490000.00,
        DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 DAY), 4,
        'Robot hút bụi có chức năng lau sàn, hỗ trợ vệ sinh tự động.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Robot_Vacuum_2016_%2831606445890%29.jpg/500px-Robot_Vacuum_2016_%2831606445890%29.jpg',
        'Ryan McBride · CC BY 2.0',
        'https://commons.wikimedia.org/wiki/File:Robot_Vacuum_2016_(31606445890).jpg',
        'Vệ sinh nhà cửa'
    UNION ALL SELECT
        'Máy hút bụi robot mini', 2990000.00, NULL, NULL, NULL, 12,
        'Robot hút bụi mini phù hợp căn hộ và không gian nhỏ.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7a/RVC3N_Robotic_Vacuum_Cleaner_%22Kreng%22_on_the_job_%288499549941%29.png/500px-RVC3N_Robotic_Vacuum_Cleaner_%22Kreng%22_on_the_job_%288499549941%29.png',
        'Dennis van Zuijlekom · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:RVC3N_Robotic_Vacuum_Cleaner_%22Kreng%22_on_the_job_(8499549941).png',
        'Vệ sinh nhà cửa'
    UNION ALL SELECT
        'Máy giặt cửa trước 9 kg', 7490000.00, NULL, NULL, NULL, 8,
        'Máy giặt cửa trước dung tích 9 kg cho nhu cầu giặt giũ gia đình.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/51/Pakshoma_Washing_Machine_9_Kg.jpg/500px-Pakshoma_Washing_Machine_9_Kg.jpg',
        'Tehran Engineer · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:Pakshoma_Washing_Machine_9_Kg.jpg',
        'Giặt ủi'
    UNION ALL SELECT
        'Máy giặt cửa trước 9 kg phiên bản 2', 7890000.00, NULL, NULL, NULL, 6,
        'Máy giặt cửa trước cho gia đình, dung tích tham khảo 9 kg.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/8/81/Pakshoma_Washing_Machine_9_Kg_2.jpg/500px-Pakshoma_Washing_Machine_9_Kg_2.jpg',
        'Tehran Engineer · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:Pakshoma_Washing_Machine_9_Kg_2.jpg',
        'Giặt ủi'
    UNION ALL SELECT
        'Máy giặt Panasonic cửa trước', 8990000.00, NULL, NULL, NULL, 5,
        'Máy giặt cửa trước, ảnh tham khảo một mẫu máy giặt Panasonic.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/8/87/Japanese_washing_machine_-_Denryokukan.jpg/500px-Japanese_washing_machine_-_Denryokukan.jpg',
        'Peter Van den Bossche · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:Japanese_washing_machine_-_Denryokukan.jpg',
        'Giặt ủi'
    UNION ALL SELECT
        'Bộ mở rộng Wi-Fi gia đình', 590000.00, NULL, NULL, NULL, 20,
        'Thiết bị hỗ trợ mở rộng vùng phủ sóng Wi-Fi trong nhà.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/15/TP-Link_TL-WR740N_router_HS5.jpg/500px-TP-Link_TL-WR740N_router_HS5.jpg',
        'Hayden Schiff · CC BY 4.0',
        'https://commons.wikimedia.org/wiki/File:TP-Link_TL-WR740N_router_HS5.jpg',
        'Thiết bị thông minh'
    UNION ALL SELECT
        'Ổ cắm thông minh điều khiển từ xa', 390000.00, NULL, NULL, NULL, 28,
        'Ổ cắm thông minh hỗ trợ bật tắt thiết bị từ xa.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/10/Wemo_Mini_Smart_Plug_%283750%29.jpg/500px-Wemo_Mini_Smart_Plug_%283750%29.jpg',
        'Gregory Varnum · CC BY-SA 4.0',
        'https://commons.wikimedia.org/wiki/File:Wemo_Mini_Smart_Plug_(3750).jpg',
        'Thiết bị thông minh'
    UNION ALL SELECT
        'Bóng đèn LED IKEA E14', 129000.00, NULL, NULL, NULL, 35,
        'Bóng đèn LED đui E14 ánh sáng trắng ấm, ảnh minh họa bóng đèn IKEA.',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/f/f2/2010s_IKEA_3_5W_E14_2700K_LED_light_bulb_made_in_China.jpg/500px-2010s_IKEA_3_5W_E14_2700K_LED_light_bulb_made_in_China.jpg',
        'R. Henrik Nilsson · CC BY 4.0',
        'https://commons.wikimedia.org/wiki/File:2010s_IKEA_3_5W_E14_2700K_LED_light_bulb_made_in_China.jpg',
        'Chiếu sáng'
) AS seed
JOIN categories AS category ON category.name = seed.category_name
WHERE NOT EXISTS (
    SELECT 1
    FROM products AS existing
    WHERE existing.name = seed.name
      AND existing.category_id = category.id
);

INSERT INTO product_images (product_id, image_url, image_attribution, image_source_url)
SELECT product.id, seed.image_url, seed.image_attribution, seed.image_source_url
FROM (
    SELECT
        'Lò nướng để bàn 12L' AS name,
        'Nhà bếp' AS category_name,
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/5/51/Hamilton_Beach_Toaster_Oven_Model_31300_-_Door_open.jpg/500px-Hamilton_Beach_Toaster_Oven_Model_31300_-_Door_open.jpg' AS image_url,
        'Infrogmation · CC BY-SA 4.0' AS image_attribution,
        'https://commons.wikimedia.org/wiki/File:Hamilton_Beach_Toaster_Oven_Model_31300_-_Door_open.jpg' AS image_source_url
    UNION ALL SELECT
        'Robot hút bụi tự động', 'Vệ sinh nhà cửa',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/Robot_Vacuum_2016_%2831606445890%29.jpg/500px-Robot_Vacuum_2016_%2831606445890%29.jpg',
        'Ryan McBride · CC BY 2.0',
        'https://commons.wikimedia.org/wiki/File:Robot_Vacuum_2016_(31606445890).jpg'
    UNION ALL SELECT
        'Máy giặt cửa trước 9 kg', 'Giặt ủi',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/8/81/Pakshoma_Washing_Machine_9_Kg_2.jpg/500px-Pakshoma_Washing_Machine_9_Kg_2.jpg',
        'Tehran Engineer · CC BY-SA 2.0',
        'https://commons.wikimedia.org/wiki/File:Pakshoma_Washing_Machine_9_Kg_2.jpg'
    UNION ALL SELECT
        'Router Wi-Fi 6 gia đình', 'Thiết bị thông minh',
        'https://thumb.wikimedia.org/wikipedia/commons/thumb/1/15/TP-Link_TL-WR740N_router_HS5.jpg/500px-TP-Link_TL-WR740N_router_HS5.jpg',
        'Hayden Schiff · CC BY 4.0',
        'https://commons.wikimedia.org/wiki/File:TP-Link_TL-WR740N_router_HS5.jpg'
) AS seed
JOIN categories AS category ON category.name = seed.category_name
JOIN products AS product ON product.name = seed.name AND product.category_id = category.id
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images AS existing
    WHERE existing.product_id = product.id
      AND existing.image_url = seed.image_url
);

-- Only add specifications directly supported by the seeded product name/description.
-- More detailed values should come from verified supplier or manufacturer data.
INSERT INTO product_specifications
    (product_id, spec_key, label, value, unit, display_order)
SELECT product.id, seed.spec_key, seed.label, seed.value, seed.unit, seed.display_order
FROM (
    SELECT 'Nồi chiên không dầu 5L' AS product_name, 'dung-tich' AS spec_key,
        'Dung tích' AS label, '5' AS value, 'L' AS unit, 0 AS display_order
    UNION ALL SELECT 'Máy lạnh Inverter 1 HP', 'cong-suat', 'Công suất', '1', 'HP', 0
    UNION ALL SELECT 'Máy lạnh Inverter 1.5 HP', 'cong-suat', 'Công suất', '1.5', 'HP', 0
    UNION ALL SELECT 'Máy lạnh Inverter 2 HP', 'cong-suat', 'Công suất', '2', 'HP', 0
    UNION ALL SELECT 'Smart TV 4K 55 inch', 'do-phan-giai', 'Độ phân giải', '4K', NULL, 0
    UNION ALL SELECT 'Smart TV 4K 55 inch', 'kich-thuoc-man-hinh', 'Kích thước màn hình', '55', 'inch', 1
    UNION ALL SELECT 'Ổ cắm thông minh Wi-Fi', 'ket-noi', 'Kết nối', 'Wi-Fi', NULL, 0
    UNION ALL SELECT 'Đèn thông minh RGB', 'mau-sac', 'Màu sắc', 'RGB', NULL, 0
    UNION ALL SELECT 'Ấm siêu tốc Bosch 1.7L', 'dung-tich', 'Dung tích', '1.7', 'L', 0
    UNION ALL SELECT 'Lò vi sóng điện tử 20L', 'dung-tich', 'Dung tích', '20', 'L', 0
    UNION ALL SELECT 'Lò nướng để bàn 12L', 'dung-tich', 'Dung tích', '12', 'L', 0
    UNION ALL SELECT 'Nồi cơm điện 1.8L', 'dung-tich', 'Dung tích', '1.8', 'L', 0
    UNION ALL SELECT 'Quạt bàn 3 tốc độ', 'so-toc-do', 'Số tốc độ', '3', NULL, 0
    UNION ALL SELECT 'Smart TV 4K 43 inch', 'do-phan-giai', 'Độ phân giải', '4K', NULL, 0
    UNION ALL SELECT 'Smart TV 4K 43 inch', 'kich-thuoc-man-hinh', 'Kích thước màn hình', '43', 'inch', 1
    UNION ALL SELECT 'Smart TV 4K 65 inch', 'do-phan-giai', 'Độ phân giải', '4K', NULL, 0
    UNION ALL SELECT 'Smart TV 4K 65 inch', 'kich-thuoc-man-hinh', 'Kích thước màn hình', '65', 'inch', 1
    UNION ALL SELECT 'Router Wi-Fi băng tần kép', 'bang-tan', 'Băng tần', 'Kép', NULL, 0
    UNION ALL SELECT 'Router Wi-Fi 6 gia đình', 'chuan-wi-fi', 'Chuẩn Wi-Fi', 'Wi-Fi 6', NULL, 0
    UNION ALL SELECT 'Máy lọc không khí HEPA', 'bo-loc', 'Bộ lọc', 'HEPA', NULL, 0
    UNION ALL SELECT 'Robot hút bụi lau nhà', 'chuc-nang', 'Chức năng', 'Hút bụi và lau nhà', NULL, 0
    UNION ALL SELECT 'Máy giặt cửa trước 9 kg', 'khoi-luong-giat', 'Khối lượng giặt', '9', 'kg', 0
    UNION ALL SELECT 'Máy giặt cửa trước 9 kg phiên bản 2', 'khoi-luong-giat', 'Khối lượng giặt', '9', 'kg', 0
    UNION ALL SELECT 'Bộ mở rộng Wi-Fi gia đình', 'ket-noi', 'Kết nối', 'Wi-Fi', NULL, 0
    UNION ALL SELECT 'Ổ cắm thông minh điều khiển từ xa', 'tinh-nang', 'Tính năng', 'Điều khiển từ xa', NULL, 0
    UNION ALL SELECT 'Bóng đèn LED IKEA E14', 'duoi-den', 'Đui đèn', 'E14', NULL, 0
) AS seed
JOIN products AS product ON product.name = seed.product_name
WHERE NOT EXISTS (
    SELECT 1
    FROM product_specifications AS existing
    WHERE existing.product_id = product.id
      AND existing.spec_key = seed.spec_key
);
