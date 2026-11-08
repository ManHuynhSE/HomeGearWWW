# HomeGear

Ứng dụng thương mại điện tử thiết bị gia đình, xây dựng bằng Spring Boot, Thymeleaf, Spring Data JPA và MariaDB. Có thể chạy local trực tiếp, không cần Docker.

## Yêu cầu

- JDK 17
- MariaDB đang chạy trên máy
- PowerShell trên Windows (các lệnh bên dưới dùng Maven Wrapper đi kèm dự án)
- API key Google AI nếu muốn bật chatbot và tìm kiếm ngữ nghĩa

## 1. Tạo database

Kết nối MariaDB bằng HeidiSQL, DBeaver hoặc MariaDB client rồi chạy:

```sql
CREATE DATABASE demoprj
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Ứng dụng tự tạo/cập nhật các bảng nghiệp vụ ở chế độ local. Không cần cài Maven riêng; dự án có `mvnw.cmd`.

## 2. Cấu hình môi trường

Tạo file `.env` từ mẫu:

```powershell
Copy-Item .env.example .env
```

Mở `.env`, điền thông tin MariaDB của máy. `AI_API_KEY` chỉ cần thiết khi chạy AI:

```properties
DB_URL=jdbc:mariadb://localhost:3306/demoprj
DB_USERNAME=root
DB_PASSWORD=mat-khau-mariadb
AI_API_KEY=google-ai-api-key
```

Không commit `.env`; file này đã được Git ignore. Có thể bỏ qua hoặc để trống `AI_API_KEY` nếu chỉ chạy các tính năng cửa hàng không dùng AI.

## 3. Chạy ứng dụng

**Chạy cơ bản, AI tắt:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Chạy với AI và vector store:**

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
```

Sau khi ứng dụng khởi động, mở <http://localhost:8080>.

Khi database chưa có tài khoản, ứng dụng tạo tài khoản quản trị demo:

```text
Email: admin@demoprj.local
Mật khẩu: Admin@123
```

Đây là thông tin demo; hãy đổi mật khẩu trước khi sử dụng môi trường thật.

## 4. Dữ liệu mẫu

Nếu bảng tài khoản chưa có dữ liệu, ứng dụng tạo tài khoản Admin demo. Nếu bảng danh mục trống, ứng dụng tạo hai danh mục và sản phẩm mẫu. Có thể nạp thêm catalog bằng `src/main/resources/db/seed-homegear.sql` sau lần chạy đầu tiên, khi các bảng đã được tạo.

Nếu chạy profile `dev` và đã có vector dữ liệu trước khi nạp catalog mẫu, gọi endpoint quản trị `POST /admin/ai/reindex` sau khi seed để cập nhật chỉ mục. Nếu chưa có vector dữ liệu, ứng dụng tự chạy ETL khi khởi động.

## AI và vector store

Profile `dev` bật Google Gemini và MariaDB Vector Store. Cần cấu hình `AI_API_KEY`; các lệnh embedding/chat có thể dùng quota của Google AI. Khi ứng dụng khởi động, nếu bảng `vector_store` chưa có dòng nào, ETL sản phẩm và chính sách sẽ tự chạy nền. Chờ log `AI vector reindex completed` trước khi kiểm tra tìm kiếm. Nếu bảng đã có dữ liệu, startup không xóa hoặc nạp lại.

Vector store dùng model `gemini-embedding-001`, 768 chiều và bảng `vector_store`. Đổi model hoặc số chiều cần tạo bảng tương thích và nạp lại dữ liệu. Có thể chủ động nạp lại bằng endpoint chỉ dành cho Admin: `POST /admin/ai/reindex`.

Chat cửa hàng truy xuất tối đa bốn đoạn chính sách ở mỗi lượt, ngưỡng tương đồng 0.65. Điều này bổ sung căn cứ cho câu trả lời nhưng phát sinh embedding/search ở mỗi lượt.

Bài kiểm thử semantic search/RAG gọi provider thật và tiêu thụ quota; chỉ chạy khi đã cấu hình khóa và muốn thực hiện smoke test:

```powershell
.\mvnw.cmd "-Dai.smoke=true" "-Dtest=AiApiSmokeTest#storeChatUsesSemanticProductSearchAndRetrievedPolicy" test
```

## Kiểm thử

```powershell
.\mvnw.cmd test
```

Các bài kiểm thử tích hợp cần kết nối MariaDB theo cấu hình trong `.env`.

## Profile production

Profile `prod` yêu cầu `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `AI_API_KEY`; Hibernate dùng `ddl-auto: validate`, vector schema không tự khởi tạo. Trước khi chạy, cần chuẩn bị đầy đủ schema nghiệp vụ và áp dụng các script:

- `src/main/resources/db/product_active.sql`
- `src/main/resources/db/vector_store_mariadb.sql`

Chỉ chạy production khi database đã được sao lưu và schema đã sẵn sàng.

Lệnh chạy profile production:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=prod" spring-boot:run
```

## Ghi chú triển khai

- Lịch sử chat lưu trong bộ nhớ tiến trình, tối đa 20 tin nhắn; mất khi ứng dụng khởi động lại. Chat Guest không tiếp nối sau khi đăng nhập.
- Endpoint chat yêu cầu CSRF và giới hạn 10 yêu cầu/phút theo tài khoản hoặc IP khách vãng lai. Giới hạn được lưu trong từng tiến trình; nếu triển khai nhiều instance, cần dùng rate-limit store dùng chung.
- Input chat tối đa 500 ký tự; provider timeout 30 giây; số lần gọi tool và output token được giới hạn.
- Tool audit log ghi tên tool và ID tài khoản, không ghi nội dung yêu cầu.
- API key lấy từ `AI_API_KEY`; không lưu khóa trong source code.
