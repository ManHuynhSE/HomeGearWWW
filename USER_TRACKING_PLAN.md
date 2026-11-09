# Kế hoạch triển khai User Tracking (theo dõi hành vi người dùng)

Dự án: HomeGear · Stack: Spring Boot 4.1.1, Java 17, Thymeleaf, Spring Security, JPA, MariaDB 12.2

## 0. Mục tiêu và phạm vi

**Mục tiêu:** ghi nhận hành vi xem, click, thêm giỏ hàng của người dùng để:

1. Hiển thị **Top 5 sản phẩm được quan tâm nhất** (theo lượt click và thêm giỏ) trên dashboard admin.
2. Cho **AI Assistant** (admin) và **AI Chatbot** (khách) trả lời được các câu hỏi về mức độ quan tâm sản phẩm.
3. Là nền cho gợi ý sản phẩm sau này.

**Ngoài phạm vi:** theo dõi chuyển động chuột, heatmap, theo dõi chéo nhiều trang web, quảng cáo.

### Các sự kiện theo dõi

| Sự kiện | Khi nào ghi | Ghi ở đâu | Lý do |
|---|---|---|---|
| `PRODUCT_VIEW` | Mở trang chi tiết sản phẩm | Server (controller) | Chính xác, không phụ thuộc JS hay trình chặn quảng cáo |
| `PRODUCT_CLICK` | Bấm vào thẻ sản phẩm (danh sách, tìm kiếm, gợi ý chatbot, trang chủ) | Front-end (JS) | Server không biết người dùng bấm từ đâu trước khi chuyển trang |
| `ADD_TO_CART` | Thêm vào giỏ **thành công** | Server (controller) | Chỉ ghi khi thao tác hợp lệ (còn hàng, đang kinh doanh) |

Đơn đã mua đã có trong `order_items`, không ghi lại.

### Quy tắc chốt từ đầu

| Quy tắc | Giá trị mặc định (để trong cấu hình) |
|---|---|
| Chống đếm trùng VIEW/CLICK | Cùng người + sản phẩm + loại sự kiện trong **30 phút** chỉ ghi 1 lần |
| ADD_TO_CART | Ghi mỗi lần thêm thành công (không chống trùng) |
| Điểm quan tâm | `score = click × 1 + add_to_cart × 3` (hệ số cấu hình được) |
| Khoảng thời gian dashboard | 7 ngày, cho chọn 30 ngày hoặc tất cả |
| Loại trừ | Admin, bot/crawler, sản phẩm không tồn tại hoặc đã ngừng kinh doanh |
| Lưu trữ | Giữ 90 ngày, sau đó xóa |
| Danh tính người xem | `user_id` nếu đã đăng nhập, ngược lại `session_hash` (băm, không lưu session id thô, không lưu IP) |

---

## Kiến trúc tổng quan

```
Trang cửa hàng (Thymeleaf)
 ├─ data-track="PRODUCT_CLICK" ──► tracker.js ──► POST /api/track ─┐
 ├─ GET /products/{id}   ───────────────────────────────────────────┤
 └─ POST /cart/add       ───────────────────────────────────────────┤
                                                                    ▼
                                            TrackingService.record() [@Async]
                                              kiểm tra loại trừ + chống trùng
                                                                    ▼
                                                  product_events (MariaDB)
                                                                    ▼
                                            AnalyticsService (tổng hợp, tính điểm)
                                              ┌─────────────┴─────────────┐
                                              ▼                           ▼
                                    Dashboard admin (Top 5)        AI tools (Assistant, Chatbot)
```

### Cấu trúc package đề xuất

```
<base-package>
└── tracking
    ├── config      TrackingProperties, AsyncConfig
    ├── model       ProductEvent, EventType, EventSource
    ├── repository  ProductEventRepository
    ├── service     TrackingService, AnalyticsService, SessionHasher, BotDetector
    ├── web         TrackingController (/api/track), dto/
    ├── job         EventRetentionJob
    └── dev         DevEventSeeder (chỉ profile dev)
resources/static/js/tracker.js
```

---

## Phase 0: Chốt thiết kế và cấu hình

**Mục tiêu:** thống nhất định nghĩa số liệu trước khi viết code, tránh sửa về sau.

- [x] Chốt danh sách sự kiện, hệ số điểm, thời gian chống trùng, thời hạn lưu trữ (bảng ở mục 0)
- [x] Tạo `TrackingProperties` (`@ConfigurationProperties("app.tracking")`):
  - `enabled` (bật/tắt toàn bộ tracking)
  - `dedupe-window-minutes` (30)
  - `weight.click` (1), `weight.add-to-cart` (3)
  - `retention-days` (90)
  - `rate-limit-per-minute` (60)
  - `bot-keywords` (bot, crawler, spider, slurp, headless...)
- [x] Tạo biến môi trường `TRACKING_SALT` (chuỗi bí mật dùng khi băm session id). Thêm vào `.env.example`, **không commit giá trị thật**
- [x] Bật `@EnableAsync` (đã có ở `VectorIndexConfiguration`); bật `@EnableScheduling` cùng phase triển khai job dọn dữ liệu
- [x] Định nghĩa công thức "tỷ lệ chuyển đổi" là `số lượng đã mua (đơn không bị hủy) / số lượt xem × 100%` trong cùng khoảng `[from, to)` theo `Asia/Ho_Chi_Minh`; đây là chỉ số tham khảo, có thể lớn hơn 100% khi mỗi lượt xem dẫn đến mua nhiều sản phẩm

**Hoàn thành khi:** có `TrackingProperties` đọc được cấu hình, tài liệu định nghĩa số liệu được ghi lại.
**Ước lượng:** 0,5 ngày

---

## Phase 1: Tầng dữ liệu

**Mục tiêu:** có bảng `product_events` và repository.

- [x] Enum `EventType`: `PRODUCT_VIEW`, `PRODUCT_CLICK`, `ADD_TO_CART`
- [x] Enum `EventSource`: `HOME`, `LISTING`, `SEARCH`, `CHATBOT`, `DETAIL`
- [x] Entity `ProductEvent` (map bảng bên dưới); lưu `eventType`, `source` bằng `@Enumerated(EnumType.STRING)`
- [x] **Không đặt khóa ngoại** `product_id` và `user_id`: tracking không được phép chặn nghiệp vụ (xóa sản phẩm, xóa tài khoản). Dữ liệu mồ côi sẽ được dọn bởi job và bị loại khi join lúc thống kê
- [x] `ProductEventRepository`:
  - `existsBy...` kiểm tra trùng trong cửa sổ thời gian (theo user hoặc session)
  - truy vấn đếm theo `(product_id, event_type)` trong khoảng thời gian (xem Phase 5)
  - `deleteByCreatedAtBefore`
- [x] Nếu schema do script SQL quản lý (prod), thêm script `src/main/resources/db/product_events.sql`

```sql
CREATE TABLE product_events (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id   BIGINT       NOT NULL,
  event_type   VARCHAR(20)  NOT NULL,   -- PRODUCT_VIEW | PRODUCT_CLICK | ADD_TO_CART
  user_id      BIGINT       NULL,       -- null nếu là Guest
  session_hash CHAR(64)     NOT NULL,   -- SHA-256, không lưu session id thô
  source       VARCHAR(20)  NULL,
  created_at   DATETIME     NOT NULL,
  INDEX idx_pe_product_type_time (product_id, event_type, created_at),
  INDEX idx_pe_time (created_at),
  INDEX idx_pe_dedupe_session (session_hash, product_id, event_type, created_at),
  INDEX idx_pe_dedupe_user (user_id, product_id, event_type, created_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**Hoàn thành khi:** ứng dụng khởi động tạo/nhận đúng bảng và index; repository chạy được test cơ bản.
**Ước lượng:** 0,5–1 ngày

---

## Phase 2: TrackingService

**Mục tiêu:** một điểm duy nhất để ghi sự kiện, đã gom đủ quy tắc loại trừ và chống trùng.

- [x] `SessionHasher`: `SHA-256(sessionId + TRACKING_SALT)` dạng hex 64 ký tự
- [x] `BotDetector`: kiểm tra `User-Agent` với danh sách `bot-keywords`; User-Agent rỗng cũng coi là nghi vấn
- [x] `TrackingService.record(productId, type, source)`:
  1. Nếu `app.tracking.enabled=false` thì thoát
  2. Bỏ qua nếu người dùng hiện tại là **ADMIN**
  3. Bỏ qua nếu là bot
  4. Kiểm tra sản phẩm **tồn tại và đang kinh doanh** (nên dùng cache nhỏ hoặc truy vấn `existsById` rất nhẹ)
  5. Với VIEW và CLICK: kiểm tra trùng trong `dedupe-window-minutes`. Khóa chống trùng là `user_id` nếu đã đăng nhập, ngược lại `session_hash` (vì session id đổi sau khi đăng nhập, dùng `user_id` giúp không đếm đôi)
  6. Lưu `ProductEvent` với `created_at` lấy từ server
- [x] Tách rõ: lấy HttpServletRequest/SecurityContext ở tầng web thông qua `TrackingContextFactory`, truyền ngữ cảnh gọn (`userId`, `sessionHash`, `isBot`, `isAdmin`) để service an toàn khi chạy `@Async`
- [x] Đánh dấu `@Async` với executor riêng (`trackingExecutor`), queue 500; khi đầy bỏ sự kiện và ghi cảnh báo, không chặn request
- [x] Mọi lỗi trong `record` chỉ ghi log, **không ném ra ngoài** (tracking lỗi không được làm hỏng trang)
- [x] Unit test quy tắc admin, bot, sản phẩm không active, chống trùng VIEW/CLICK và không chống trùng ADD_TO_CART

**Hoàn thành khi:** gọi `record` hai lần liên tiếp cùng ngữ cảnh chỉ ra một dòng cho VIEW/CLICK, hai dòng cho ADD_TO_CART; admin và bot không sinh dòng nào.
**Ước lượng:** 1–1,5 ngày

---

## Phase 3: Ghi sự kiện phía server và endpoint `/api/track`

**Mục tiêu:** VIEW và ADD_TO_CART được ghi tự động; CLICK nhận từ client một cách an toàn.

**Gắn vào controller sẵn có**
- [x] Controller chi tiết sản phẩm: gọi `record(id, PRODUCT_VIEW, DETAIL)` (nếu có tham số `?src=` hợp lệ có thể dùng làm `source`)
- [x] Controller giỏ hàng: gọi `record(id, ADD_TO_CART, <source>)` **sau khi** kiểm tra tồn kho và thêm thành công, không ghi khi bị từ chối
- [x] Không để `record` nằm trong cùng transaction nghiệp vụ nếu có thể làm rollback ảnh hưởng nhau (dùng `@Async` đã tách luồng)

**Endpoint nhận click từ client**
- [x] `POST /api/track` nhận `{ "event": "PRODUCT_CLICK", "productId": 12, "source": "LISTING" }`
- [x] Chỉ chấp nhận `event = PRODUCT_CLICK`; `PRODUCT_VIEW` và `ADD_TO_CART` do server tự ghi, **không tin client**
- [x] Kiểm tra: `productId` hợp lệ, `source` thuộc `EventSource`, thân request nhỏ (giới hạn kích thước)
- [x] Giới hạn tần suất theo session (`rate-limit-per-minute`), vượt thì bỏ qua
- [x] Luôn trả `204 No Content` kể cả khi bị bỏ qua (không tiết lộ lý do cho kẻ thử)
- [x] Spring Security: cho phép Guest và Customer, **giữ CSRF bật**, endpoint thuộc luồng form/JS thường
- [x] Nếu cấu hình Security sẵn có chặn `/api/**`, thêm quy tắc cho `/api/track`

**Hoàn thành khi:** mở trang chi tiết và thêm giỏ sinh đúng sự kiện; gọi `/api/track` với `event=ADD_TO_CART` hoặc `productId` không tồn tại không ghi gì và vẫn trả 204.
**Ước lượng:** 1 ngày

---

## Phase 4: Front-end tracker

**Mục tiêu:** ghi click không cần sửa JS ở từng trang.

- [x] Layout chung có hai thẻ meta CSRF (`th:content="${_csrf.token}"` và tên header) để JS đọc
- [x] Gắn thuộc tính vào mọi liên kết/thẻ sản phẩm: `data-track="PRODUCT_CLICK"`, `data-product-id`, `data-source` (`HOME`, `LISTING`, `SEARCH`...)
- [x] Viết `static/js/tracker.js` dùng **event delegation** trên `document`:
  - tìm phần tử gần nhất có `[data-track]`
  - gửi `fetch('/api/track', { method: 'POST', keepalive: true, headers: {Content-Type, <csrf header>}, body })`
  - `.catch(() => {})` để lỗi tracking không ảnh hưởng người dùng
  - dùng `keepalive` thay vì `sendBeacon` vì `sendBeacon` không gửi được header CSRF
- [x] Nhúng `tracker.js` vào layout dùng chung (một lần)
- [x] Gắn `data-source="CHATBOT"` cho link sản phẩm trong khung chat: khi render tin nhắn của AI (đã quy định chỉ cho phép link nội bộ `/products/{id}`), tạo thẻ `<a>` bằng DOM API và đặt `dataset.track`, `dataset.productId`, `dataset.source`
- [x] Không dùng thư viện tracking bên thứ ba, không cookie theo dõi riêng
- [x] Dùng sự kiện `click` chuẩn của trình duyệt để hỗ trợ chạm trên thiết bị di động

**Hoàn thành khi:** bấm vào sản phẩm ở trang chủ, danh sách, kết quả tìm kiếm và link trong chatbot đều sinh `PRODUCT_CLICK` với `source` đúng; tắt JS không làm hỏng điều hướng.
**Ước lượng:** 1 ngày

---

## Phase 5: AnalyticsService (tổng hợp số liệu)

**Mục tiêu:** một lớp cung cấp mọi số liệu tổng hợp cho dashboard và AI.

- [x] Truy vấn đếm theo `product_id` và `event_type` trong khoảng `[from, to)`. Chọn **nhóm theo cả hai cột rồi gộp trong Java** để hệ số điểm lấy từ cấu hình (không viết cứng trong truy vấn)
- [x] Trả về `ProductEngagement` (record/DTO):
  `productId, productName, imageUrl, views, clicks, addToCart, score, purchasedQuantity, conversionRate`
- [x] `getTopProducts(from, to, limit, metric)` với `metric` ∈ `SCORE | CLICKS | ADD_TO_CART | VIEWS`; mặc định `SCORE`; sắp xếp giảm dần, hòa điểm thì ưu tiên mới hơn hoặc tên
- [x] `getProductEngagement(productId hoặc tên, from, to)`
- [x] `getFunnel(from, to)`: tổng lượt xem → tổng thêm giỏ → số lượng đã mua (từ `order_items` của đơn không bị hủy)
- [x] `getLowConversionProducts(from, to, minViews, limit)`: xem nhiều nhưng ít mua (gợi ý cho admin xem lại giá/mô tả)
- [x] `getTrendingProducts(limit)`: top theo điểm trong 7 ngày, **chỉ sản phẩm đang kinh doanh và còn hàng**
- [x] `getRecentlyViewed(limit)`: sản phẩm gần đây của khách đã đăng nhập; userId được lấy từ `SecurityContext`
- [x] Chỉ join với sản phẩm còn tồn tại (bỏ dữ liệu mồ côi)
- [x] Xử lý chia cho 0 khi tính tỷ lệ chuyển đổi (trả `null` hoặc 0, hiển thị "—")
- [x] Khoảng thời gian: dùng nửa mở `[from, to)` thống nhất, tính theo múi giờ cửa hàng (Asia/Ho_Chi_Minh)
- [x] Unit/integration test với dữ liệu mẫu biết trước kết quả

**Hoàn thành khi:** với bộ dữ liệu mẫu, `getTopProducts` trả đúng thứ tự và đúng điểm theo công thức; đổi hệ số trong cấu hình thì thứ tự đổi theo.
**Ước lượng:** 1,5 ngày

---

## Phase 6: Dashboard admin

**Mục tiêu:** hiển thị số liệu trên trang Tổng quan có sẵn.

- [x] Thẻ **"Top 5 sản phẩm được quan tâm"**: thứ hạng, ảnh nhỏ, tên (link sang trang sửa sản phẩm), lượt xem, click, thêm giỏ, điểm, thanh ngang thể hiện điểm so với sản phẩm đứng đầu
- [x] Bộ chọn khoảng thời gian 7 ngày / 30 ngày / tất cả, gửi bằng tham số GET (`?days=7`) và render phía server; không cần JS phức tạp
- [x] Bộ chọn tiêu chí: Điểm / Click / Thêm giỏ / Lượt xem
- [x] Thẻ **phễu**: Lượt xem → Thêm giỏ → Đã mua, kèm phần trăm giữa các bước
- [x] (Tùy chọn) bảng **"Xem nhiều nhưng ít mua"**
- [x] Trạng thái rỗng: chưa có dữ liệu thì hiện thông báo thay vì bảng trống
- [x] Thanh ngang dùng CSS thuần; nếu muốn biểu đồ thì nạp Chart.js từ CDN
- [x] Ghi chú nhỏ trên giao diện: công thức điểm và khoảng thời gian đang dùng
- [ ] Tối ưu: kết quả top có thể cache ngắn hạn (1–5 phút) nếu truy vấn chậm
- [x] Giao diện thống nhất với các thẻ hiện có (xanh dương, bo góc, cùng khoảng cách), responsive

**Hoàn thành khi:** dashboard hiển thị đúng Top 5 khớp với dữ liệu trong bảng, đổi bộ lọc thì số liệu đổi theo, trang không lỗi khi chưa có dữ liệu.
**Ước lượng:** 1,5 ngày

---

## Phase 7: Tích hợp AI (tool)

**Mục tiêu:** AI biết số liệu hành vi qua tool. Đây là số liệu có cấu trúc nên **không nạp vào vector store**.

Bổ sung vào `AdminTools` và `StoreTools` (xem `AI_INTEGRATION_PLAN.md`, Phase 2), mỗi tool chỉ gọi `AnalyticsService`:

| Tool | Dành cho | Tham số | Trả về |
|---|---|---|---|
| `getTopViewedProducts` | Assistant (admin) | fromDate, toDate, limit, metric | Top sản phẩm kèm views/clicks/addToCart/score |
| `getProductEngagement` | Assistant (admin) | productName, fromDate, toDate | Chỉ số một sản phẩm, kèm tỷ lệ chuyển đổi |
| `getEngagementFunnel` | Assistant (admin) | fromDate, toDate | Phễu xem → giỏ → mua |
| `getTrendingProducts` | Chatbot (khách) | limit | Sản phẩm đang được quan tâm, chỉ số tổng hợp, còn hàng |
| `getMyRecentlyViewed` | Chatbot (khách đã đăng nhập) | limit | Sản phẩm người này xem gần đây |

- [ ] Admin tools có `@PreAuthorize("hasRole('ADMIN')")` ở tầng service
- [ ] `getMyRecentlyViewed` **không nhận userId từ tham số**, lấy từ `SecurityContext`; Guest thì trả thông điệp cần đăng nhập
- [ ] Tool của khách chỉ trả số liệu **đã gộp**, tuyệt đối không có sự kiện của người khác hay `session_hash`
- [ ] Ngày tháng nhận định dạng ISO; thiếu khoảng thời gian thì AI hỏi lại (đã có trong system prompt của Assistant)
- [ ] Cập nhật system prompt: giải thích định nghĩa số liệu (điểm = click × 1 + thêm giỏ × 3, mặc định 7 ngày) để AI giải thích đúng khi được hỏi, và dặn AI không tự suy diễn khi chưa có dữ liệu
- [ ] Giới hạn số dòng trả về (tối đa 10–20)
- [ ] Unit test từng tool (service mock) và test phân quyền

**Hoàn thành khi:** admin hỏi "Top 5 sản phẩm được quan tâm tuần này" nhận đúng danh sách khớp dashboard; khách hỏi "sản phẩm nào đang hot" nhận kết quả hợp lệ; Guest hỏi "tôi xem gì gần đây" được yêu cầu đăng nhập.
**Ước lượng:** 1 ngày

---

## Phase 8: Quyền riêng tư và bảo mật

**Mục tiêu:** thu thập tối thiểu, bảo vệ dữ liệu, dọn dữ liệu cũ.

- [ ] Không lưu session id thô, không lưu IP, không lưu nội dung nhập của người dùng
- [ ] `TRACKING_SALT` chỉ qua biến môi trường
- [ ] `EventRetentionJob` (`@Scheduled`, chạy hằng ngày vào giờ thấp điểm): xóa sự kiện cũ hơn `retention-days`, xóa theo lô để không khóa bảng lâu; ghi log số dòng đã xóa
- [ ] Job dọn dữ liệu mồ côi (sản phẩm đã bị xóa)
- [ ] Bổ sung đoạn mô tả vào trang chính sách quyền riêng tư: cửa hàng thu thập dữ liệu lượt xem ẩn danh để cải thiện trải nghiệm và thống kê
- [ ] Bảo vệ `/api/track`: CSRF, chỉ chấp nhận `PRODUCT_CLICK`, giới hạn tần suất, giới hạn kích thước body
- [ ] Trang dashboard và AI Assistant chỉ dành cho `ROLE_ADMIN`
- [ ] Kiểm tra lại rằng không có endpoint nào trả danh sách sự kiện thô cho client
- [ ] Cờ `app.tracking.enabled` để tắt nhanh toàn bộ tracking khi cần

**Hoàn thành khi:** kiểm tra CSDL không thấy session id/IP thô; job xóa đúng các dòng quá hạn; client không đọc được dữ liệu sự kiện của người khác.
**Ước lượng:** 1 ngày

---

## Phase 9: Kiểm thử và dữ liệu demo

**Mục tiêu:** có bằng chứng số liệu đúng và có dữ liệu để demo ngay.

**Unit test**
- [ ] Quy tắc `TrackingService` (admin, bot, sản phẩm không hợp lệ, chống trùng, ADD_TO_CART)
- [ ] `SessionHasher` ổn định với cùng đầu vào, khác nhau khi đổi salt
- [ ] Tính điểm, chuyển đổi, chia cho 0 trong `AnalyticsService`

**Integration test (Testcontainers MariaDB cùng phiên bản đang dùng)**
- [ ] Truy vấn tổng hợp đúng với dữ liệu mẫu
- [ ] Khoảng thời gian `[from, to)` đúng biên
- [ ] Job xóa dữ liệu cũ
- [ ] Controller: mở chi tiết sản phẩm sinh VIEW; thêm giỏ thành công sinh ADD_TO_CART; thêm giỏ thất bại (hết hàng) không sinh gì; F5 liên tục chỉ ra một VIEW

**Test bảo mật (MockMvc)**
- [ ] `/api/track` từ chối `event` không phải CLICK, `productId` sai (vẫn 204 và không ghi)
- [ ] Thiếu CSRF bị từ chối
- [ ] Customer không gọi được tool/endpoint admin
- [ ] Tool khách không trả dữ liệu cá nhân của người khác

**Dữ liệu demo**
- [ ] `DevEventSeeder` (`@Profile("dev")`, chỉ chạy khi bảng rỗng) sinh vài nghìn sự kiện trải trong 30 ngày với **phân bố lệch** (vài sản phẩm nổi bật, phần còn lại ít) và có cả những sản phẩm nhiều view ít mua
- [ ] Đảm bảo seeder **không chạy ở profile prod**

**Hoàn thành khi:** toàn bộ test xanh; sau khi seed, dashboard và AI đều trả lời được với số liệu hợp lý.
**Ước lượng:** 1,5–2 ngày

---

## Phase 10: Tài liệu và báo cáo

- [ ] Bổ sung vào spec: mô tả chức năng theo dõi hành vi, thống kê Top sản phẩm, các use case tương ứng (Admin xem thống kê, AI trả lời về mức độ quan tâm)
- [ ] Vẽ ERD có bảng `product_events` và sơ đồ tuần tự: click → `/api/track` → TrackingService → DB; admin hỏi AI → tool → AnalyticsService
- [ ] Ghi định nghĩa số liệu (điểm, chống trùng, khoảng thời gian, tỷ lệ chuyển đổi) vào báo cáo
- [ ] Yêu cầu phi chức năng: tracking không làm chậm trang (bất đồng bộ), không lưu dữ liệu cá nhân thô, có thời hạn lưu trữ
- [ ] Chương cài đặt: biến môi trường `TRACKING_SALT`, cấu hình `app.tracking.*`, cách chạy seeder ở dev
- [ ] Hạn chế: số liệu từ ẩn danh chỉ gần đúng (đổi trình duyệt/thiết bị tính là người khác), không phân biệt hoàn toàn bot tinh vi
- [ ] Hướng phát triển: gợi ý sản phẩm theo lịch sử xem, bảng tổng hợp theo ngày, phân tích nguồn truy cập, A/B test
- [ ] Cập nhật `MVP_PROGRESS.md` và nhật ký tuần

---

## Lịch gợi ý

| Giai đoạn | Nội dung | Thời gian |
|---|---|---|
| Ngày 1–2 | Phase 0, 1, 2 | 2–3 ngày |
| Ngày 3–4 | Phase 3, 4 | 2 ngày |
| Ngày 5–6 | Phase 5, 6 | 3 ngày |
| Ngày 7–8 | Phase 7, 8 | 2 ngày |
| Ngày 9–10 | Phase 9, 10 | 2–3 ngày |

Tổng khoảng **1,5–2 tuần**. Có thể làm song song với các phase 6–8 của kế hoạch AI vì chỉ gặp nhau ở Phase 7 (tool).

**Nếu thiếu thời gian, cắt theo thứ tự:** bảng "xem nhiều ít mua" → phễu → bộ chọn tiêu chí → tool `getMyRecentlyViewed` → job dọn dữ liệu mồ côi. **Không cắt:** chống trùng, loại trừ admin/bot, bảo vệ `/api/track`, phân quyền tool.

## Phụ lục A: Cấu hình mẫu

```yaml
app:
  tracking:
    enabled: true
    dedupe-window-minutes: 30
    weight:
      click: 1
      add-to-cart: 3
    retention-days: 90
    rate-limit-per-minute: 60
    bot-keywords: [bot, crawler, spider, slurp, headless]
```

Biến môi trường: `TRACKING_SALT` (bắt buộc ở prod).

## Phụ lục B: Hợp đồng API

```
POST /api/track
Headers: Content-Type: application/json, <CSRF header>: <token>
Body:    { "event": "PRODUCT_CLICK", "productId": 12, "source": "LISTING" }
Phản hồi: 204 No Content (luôn, kể cả khi bị bỏ qua)
```

## Phụ lục C: Gắn thuộc tính vào giao diện

```html
<a th:href="@{/products/{id}(id=${p.id})}"
   data-track="PRODUCT_CLICK"
   th:attr="data-product-id=${p.id}"
   data-source="LISTING">
  ...
</a>
```

## Định nghĩa hoàn thành tổng thể

- [ ] VIEW, CLICK, ADD_TO_CART được ghi đúng và không đếm trùng
- [ ] Admin và bot không làm sai lệch số liệu
- [ ] Dashboard hiển thị đúng Top 5 và phễu, đổi bộ lọc hoạt động
- [ ] AI Assistant và Chatbot trả lời đúng các câu hỏi về mức độ quan tâm, khớp với dashboard
- [ ] Không lưu session id thô hay IP; dữ liệu cũ được dọn tự động
- [ ] `/api/track` chống được lạm dụng; tool không rò rỉ dữ liệu giữa người dùng
- [ ] Test tự động xanh, có dữ liệu demo ở profile dev
- [ ] Báo cáo và `MVP_PROGRESS.md` đã cập nhật
