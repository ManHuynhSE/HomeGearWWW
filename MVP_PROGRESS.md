# MVP Progress

## Scope

MVP theo `spec.md`, dùng Spring Boot + Thymeleaf + MariaDB:

- Guest: xem, tìm kiếm sản phẩm và thêm vào giỏ hàng.
- Customer: đăng ký, đăng nhập, cập nhật giỏ hàng, checkout, xem lịch sử đơn hàng.
- Admin: xem/thêm/sửa/xóa danh mục và sản phẩm; xem đơn hàng.
- Bảo mật: mã hóa mật khẩu, phân quyền `CUSTOMER`/`ADMIN`, kiểm tra tồn kho và các ràng buộc xóa cơ bản.

## Progress

- [x] Khởi tạo cấu trúc Spring Boot, Thymeleaf, JPA, Security, MariaDB.
- [x] Chuẩn hóa MVC theo package phân tầng:
  - `model.entity`: các Entity JPA.
  - `model.enums`: các enum của domain.
  - `repository`: các Spring Data Repository.
  - `service`: nghiệp vụ ứng dụng.
  - `controller`: MVC Controller xử lý request và truyền dữ liệu sang View.
  - `config`: cấu hình Security và khởi tạo dữ liệu.
  - `resources/templates`: View Thymeleaf.
- [x] Thiết kế entity và repository cho tài khoản, danh mục, sản phẩm, đơn hàng.
- [x] Đăng ký, đăng nhập, đăng xuất và phân quyền.
- [x] Catalog: danh sách, tìm kiếm, chi tiết sản phẩm.
- [x] Giỏ hàng lưu trong Session.
- [x] Checkout kiểm tra tồn kho, khóa sản phẩm trong transaction, từ chối số lượng vượt kho và chỉ trừ kho khi đơn hợp lệ.
- [x] Lịch sử và chi tiết đơn hàng của Customer.
- [x] Admin CRUD danh mục và sản phẩm, kèm ràng buộc xóa.
- [x] Admin quản lý tài khoản Customer, không hiển thị mật khẩu.
- [x] Admin xem chi tiết đơn hàng, cập nhật trạng thái và xem doanh thu đơn hoàn tất.
- [x] Admin tìm kiếm/lọc sản phẩm, khách hàng và đơn hàng theo trạng thái.
- [ ] Email xác nhận.
- [ ] AI Chatbot/AI Assistant.
- [ ] Thanh toán online/VNPay và vận chuyển.
- [x] Compile, Spring context test và smoke test các luồng MVP chính.
- [ ] Hoàn thiện email, AI, thanh toán online/vận chuyển và giao diện nâng cao.

## Database connection required

Ứng dụng mặc định kết nối:

```text
URL: jdbc:mariadb://localhost:3306/demoprj
Username: root
Password: cấu hình qua biến môi trường `DB_PASSWORD`
```

Có thể ghi đè bằng biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DDL_AUTO`.
Trước khi chạy cần tạo database:

```sql
CREATE DATABASE demoprj CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Tài khoản Admin demo được tạo tự động khi database chưa có dữ liệu:

```text
Email: admin@demoprj.local
Password: Admin@123
```

Đổi mật khẩu demo ngay khi chạy môi trường thật.
