# Kế hoạch hoàn thiện và nâng cấp HomeGear

> Checklist triển khai theo thứ tự ưu tiên. Các mục `[x]` là nền tảng đã có theo hiện trạng project; các mục `[ ]` là việc cần làm.  
> Phạm vi MVP hiện tại đã có catalog cơ bản, trang chi tiết sản phẩm, giỏ hàng theo session, đặt hàng/lịch sử đơn và trang quản trị cơ bản. Các mục tương ứng bên dưới là phần hoàn thiện/nâng cấp, không phải xây lại từ đầu.

## P0 — Sửa ngay các điểm làm giao diện trông như dữ liệu demo

- [ ] Rà soát và xóa dữ liệu sản phẩm/danh mục thử nghiệm không phù hợp; seed data mới trong code đã là sản phẩm HomeGear, nhưng chưa xác nhận an toàn các bản ghi đang có trong MariaDB.
- [x] Quy định xử lý sản phẩm hết hàng: vẫn hiển thị để khách tham khảo, có nhãn **Hết hàng**, vô hiệu hóa nút thêm giỏ/mua ngay; checkout tiếp tục kiểm tra tồn kho ở backend.
- [x] Chuẩn hóa định dạng tiền Việt trên giao diện: `1.890.000 ₫` (dấu chấm phân tách hàng nghìn, ký hiệu `₫`).
- [x] Thay mã trạng thái đơn tiếng Anh trên giao diện bằng nhãn tiếng Việt, giữ nguyên mã nội bộ trong database.
- [x] Mở rộng footer bằng các liên kết đang hoạt động và thông tin cửa hàng; không tự bịa thông tin liên hệ/chính sách chưa được cấu hình.
- [x] Kiểm tra dữ liệu thử nghiệm hiện có trong MariaDB; giữ sản phẩm khớp chính xác vì đang được đơn hàng tham chiếu và không xóa danh mục `Snar` khi vẫn còn sản phẩm sử dụng.

## P1 — Hoàn thiện trang chủ và nhận diện cửa hàng

- [x] Thêm thanh thông báo **Miễn phí vận chuyển đơn từ 500.000 ₫**; ngưỡng có thể cấu hình qua `homegear.shipping.free-threshold`.
- [x] Thiết kế lại header gồm logo, ô tìm kiếm nổi bật, giỏ hàng có số lượng, tài khoản và điều hướng danh mục; không hiển thị email quản trị trên header công khai.
- [x] Xây dựng gợi ý tự động cho ô tìm kiếm, giới hạn 7 kết quả, thao tác bằng bàn phím và trạng thái rỗng/lỗi.
- [x] Thêm menu danh mục trên desktop và menu hamburger trên mobile.
- [x] Thêm dải danh mục dạng thẻ có icon và liên kết lọc theo danh mục.
- [x] Thêm dải thông tin mua hàng dựa trên tính năng hiện có; chưa quảng bá đổi trả/bảo hành/giao nhanh chưa được xác nhận.
- [x] Tổ chức sản phẩm thành Bán chạy (từ mặt hàng trong đơn không bị hủy), Mới về (ID mới nhất) và Khuyến mãi đang chạy.
- [x] Bổ sung giá gốc, thời gian khuyến mãi, phần trăm giảm và badge; dữ liệu khuyến mãi được nhập từ quản trị, không tự sinh giá giảm.
- [x] Thêm banner/countdown chỉ khi có khuyến mãi đang hoạt động và thời điểm kết thúc hợp lệ.
- [ ] Thêm đánh giá khách hàng và logo thương hiệu sau khi có dữ liệu/nguồn nội dung phù hợp.
- [ ] Thêm đăng ký nhận tin sau khi xác định dịch vụ gửi email và quy trình đồng ý sử dụng dữ liệu.
- [x] Tạo footer 4 cột và cấu hình các liên kết chính sách, liên hệ, mạng xã hội, phương thức thanh toán qua `application.properties`; mục chưa cấu hình không được hiển thị như thông tin đã xác thực.
- [x] Thêm nút liên hệ nổi Zalo/Messenger/hotline; mỗi nút chỉ hiển thị khi có thông tin tương ứng.
- [ ] Điền và xác minh thông tin liên hệ, URL chính sách/mạng xã hội và chỉ bật phương thức thanh toán đang được checkout hỗ trợ.

## P2 — Trang danh sách và khám phá sản phẩm

- [x] Tách trang chủ `/` khỏi trang catalog `/products`; bộ lọc và danh sách phân trang chỉ hiển thị trong trang sản phẩm riêng.
- [x] Duy trì trang catalog, tìm kiếm và trang chi tiết sản phẩm hiện có.
- [x] Thêm breadcrumb, tổng số kết quả và trạng thái rỗng **Không tìm thấy sản phẩm** kèm gợi ý xóa bộ lọc/đổi từ khóa.
- [x] Thêm bộ lọc danh mục, khoảng giá và tình trạng còn hàng.
- [ ] Thêm bộ lọc thương hiệu sau khi thương hiệu trở thành dữ liệu có quản lý trong hệ thống.
- [x] Thêm sắp xếp giá tăng/giảm, mới nhất và bán chạy; bán chạy tính theo số lượng trong đơn không bị hủy.
- [x] Thêm phân trang và giữ trạng thái tìm kiếm/bộ lọc khi chuyển trang.
- [x] Nâng cấp thẻ sản phẩm với nhãn Mới/giảm giá, giá, tồn kho và xem nhanh.
- [ ] Bổ sung đánh giá sao và danh sách yêu thích khi có dữ liệu đánh giá và luồng lưu yêu thích.
- [x] Thêm trường URL ảnh tùy chọn ở quản trị, lazy-load ảnh và hiển thị ảnh thay thế khi thiếu/lỗi ảnh.
- [x] Mở rộng SQL seed lên 30 sản phẩm/7 danh mục với ảnh Wikimedia Commons, liên kết nguồn và ghi công giấy phép; chỉ điền ảnh cho sản phẩm seed cũ khi chưa có ảnh.
- [x] Chuyển lọc, sắp xếp, tìm kiếm gợi ý và phân trang catalog sang truy vấn database.
- [ ] Thêm thương hiệu, đánh giá và quản lý/tải lên nhiều ảnh sản phẩm khi có nguồn dữ liệu và quy trình quản trị phù hợp.

## P3 — Trang chi tiết sản phẩm

- [x] Có template và luồng trang chi tiết sản phẩm cơ bản.
- [x] Bổ sung gallery ảnh chính/ảnh thu nhỏ, phóng to và hiển thị ghi công từng ảnh; nhiều ảnh phụ có thể được nạp qua bảng `product_images`.
- [x] Hiển thị tên, mã sản phẩm nội bộ, trạng thái đánh giá, giá hiện tại, giá gốc và phần trăm giảm khi có dữ liệu tương ứng; không tạo đánh giá giả.
- [x] Bổ sung chọn số lượng, trạng thái tồn kho chính xác, nút **Thêm vào giỏ** và **Mua ngay**; “Mua ngay” thêm vào giỏ rồi mở giỏ hàng.
- [x] Vô hiệu hóa thao tác mua khi hết hàng; kiểm tra số lượng khi thêm giỏ và kiểm tra lại tồn kho lúc tạo đơn.
- [x] Tổ chức nội dung thành tab Mô tả, Thông số kỹ thuật, Đánh giá và Chính sách; hiển thị trạng thái chưa có dữ liệu khi chưa cấu hình.
- [x] Hiển thị sản phẩm liên quan, ngưỡng miễn phí vận chuyển hiện được cấu hình và liên kết chính sách chỉ khi đã cung cấp.
- [x] Cập nhật title, meta description, canonical URL và dữ liệu có cấu trúc Product cho từng sản phẩm.
- [x] Tạo URL thân thiện theo slug; URL `/products/{id}` cũ vẫn hoạt động và trỏ canonical sang URL mới.

## P4 — Giỏ hàng, checkout và sau đặt hàng

- [x] Có giỏ hàng session, cập nhật số lượng và luồng tạo đơn cơ bản.
- [ ] Hoàn thiện giỏ hàng: tăng/giảm số lượng, xóa mặt hàng, tạm tính và tổng tiền cập nhật nhất quán.
- [ ] Thêm mã giảm giá sau khi có mô hình và quy tắc áp dụng coupon.
- [ ] Xác định cách tính phí vận chuyển, ngưỡng miễn phí ship và hiển thị rõ phí trước khi xác nhận đơn.
- [ ] Thiết kế checkout theo bước: thông tin giao hàng, vận chuyển, thanh toán, xác nhận đơn.
- [ ] Bổ sung họ tên, SĐT, địa chỉ giao hàng; tích hợp chọn Tỉnh/Quận/Phường sau khi chốt nhà cung cấp dữ liệu địa chỉ.
- [ ] Bổ sung phương thức COD và chuyển khoản; tích hợp VNPay/MoMo chỉ sau khi có tài khoản, môi trường kiểm thử và quy trình đối soát.
- [ ] Hiển thị tóm tắt đơn hàng và tổng tiền cuối cùng trước khi đặt hàng.
- [ ] Tạo trang đặt hàng thành công hiển thị mã đơn; gửi email xác nhận sau khi cấu hình dịch vụ email.
- [ ] Đánh giá hỗ trợ guest checkout: cần thiết kế định danh đơn, tra cứu đơn an toàn và liên kết tài khoản nếu khách đăng ký sau đó.
- [ ] Chống đặt hàng lặp khi người dùng tải lại/gửi lại biểu mẫu checkout.
- [ ] Hiển thị thông báo toast khi thêm/cập nhật/xóa sản phẩm khỏi giỏ.
- [ ] Bổ sung thanh mua hàng cố định ở cuối màn hình mobile tại trang chi tiết sản phẩm.

## P5 — Tài khoản khách hàng

- [x] Có đăng ký, đăng nhập, đăng xuất và lịch sử đơn hàng cơ bản.
- [ ] Bổ sung hồ sơ khách hàng và cập nhật thông tin cá nhân.
- [ ] Bổ sung sổ địa chỉ giao hàng.
- [ ] Bổ sung timeline theo dõi trạng thái cho từng đơn.
- [ ] Cho phép hủy đơn khi còn trạng thái **Mới**, có xác nhận và kiểm tra điều kiện ở backend.
- [ ] Bổ sung quên/đặt lại mật khẩu bằng liên kết có hạn dùng và email đã xác minh.
- [ ] Đánh giá đăng nhập Google; chỉ triển khai khi cấu hình OAuth và chính sách tài khoản đã rõ.
- [ ] Bổ sung danh sách yêu thích sau khi có yêu cầu nghiệp vụ và cách lưu cho khách chưa đăng nhập.

## P6 — Nâng cấp trang quản trị

- [x] Có dashboard quản trị, CRUD cơ bản danh mục/sản phẩm, tìm kiếm khách hàng/đơn hàng và cập nhật trạng thái đơn.
- [ ] Thêm liên kết từ cảnh báo tồn kho thấp đến danh sách sản phẩm đã lọc; làm rõ ngưỡng cảnh báo và cấu hình ngưỡng nếu cần.
- [ ] Bổ sung biểu đồ doanh thu theo ngày/tháng; xác định múi giờ, khoảng thời gian và chỉ tính đơn có trạng thái phù hợp.
- [ ] Bổ sung bảng sản phẩm bán chạy với tiêu chí tính toán minh bạch.
- [ ] Chuyển form thêm/sửa sản phẩm khỏi hàng nhập nhỏ sang modal hoặc trang riêng; giữ khả năng sử dụng trên mobile.
- [ ] Bổ sung upload nhiều ảnh cho sản phẩm, kiểm tra loại/kích thước tệp và quy hoạch lưu trữ.
- [ ] Bổ sung bật/tắt hiển thị sản phẩm; sản phẩm ẩn không xuất hiện công khai nhưng dữ liệu đơn cũ vẫn được bảo toàn.
- [ ] Bổ sung chọn nhiều dòng/bulk action với xác nhận và kiểm tra ràng buộc nghiệp vụ.
- [ ] Mở rộng chi tiết đơn: thông tin giao hàng, sản phẩm, ghi nhận lịch sử trạng thái và người thao tác.
- [ ] Bổ sung in hóa đơn và xuất Excel; chỉ xuất các trường dữ liệu quản trị được phép.
- [ ] Bổ sung lọc đơn theo khoảng ngày.
- [ ] Thêm phân trang và sắp xếp cho bảng dữ liệu; không tải toàn bộ bản ghi khi dữ liệu tăng.
- [ ] Thêm hộp thoại xác nhận trước khi xóa sản phẩm/danh mục/khách hàng hoặc hủy đơn.
- [ ] Tạo chức năng quản lý mã giảm giá, banner trang chủ, đánh giá, nhân viên/phân quyền và nhật ký hoạt động.
- [ ] Rà soát quyền truy cập từng chức năng admin; không để email hoặc dữ liệu tài khoản quản trị xuất hiện ở giao diện công khai.

## P7 — Hoàn thiện kỹ thuật, responsive và vận hành

- [ ] Kiểm tra responsive toàn bộ luồng trên điện thoại, tablet và desktop; bảo đảm bảng có cách cuộn/hiển thị phù hợp.
- [ ] Thêm trạng thái loading phù hợp; chỉ dùng skeleton cho nội dung tải bất đồng bộ thực sự.
- [ ] Lazy-load ảnh sản phẩm, đặt kích thước ảnh và văn bản thay thế để tránh layout shift.
- [ ] Hoàn thiện title/meta description, URL thân thiện, canonical và dữ liệu có cấu trúc cho trang công khai.
- [ ] Tạo sitemap và robots.txt; loại trang tài khoản/quản trị khỏi nội dung lập chỉ mục.
- [ ] Bổ sung trang 404, Giới thiệu, Liên hệ, FAQ và các trang chính sách đổi trả/bảo hành/vận chuyển.
- [ ] Rà soát CSRF/XSS và phân quyền; giữ CSRF cho thao tác thay đổi dữ liệu, escape nội dung người dùng và không render HTML không tin cậy.
- [ ] Bổ sung giới hạn thử đăng nhập/chống dò mật khẩu, ghi nhận sự kiện bất thường và quy trình khôi phục tài khoản.
- [ ] Cấu hình HTTPS, secrets và thông tin tích hợp qua biến môi trường khi triển khai; không dùng mật khẩu demo ở môi trường thật.
- [ ] Thêm kiểm thử tự động cho đăng ký/đăng nhập, tìm kiếm/lọc, hết hàng, checkout, quyền admin và cập nhật trạng thái đơn.
- [ ] Kiểm thử nghiệm thu trên dữ liệu sạch và trên môi trường gần production; xác nhận migration/backup trước khi phát hành.

## Thứ tự phát hành đề xuất

1. Hoàn tất **P0** và kiểm tra dữ liệu hiện có.
2. Hoàn thiện giao diện cửa hàng và catalog (**P1–P3**) cùng các trường dữ liệu còn thiếu.
3. Hoàn thiện checkout và tài khoản (**P4–P5**); tích hợp email/thanh toán/địa chỉ sau khi chọn nhà cung cấp.
4. Nâng cấp quản trị (**P6**) theo mức độ cần thiết cho vận hành thực tế.
5. Hoàn tất bảo mật, SEO, kiểm thử và triển khai (**P7**).

## Phụ thuộc cần chốt trước khi triển khai

- [ ] Nguồn ảnh, thương hiệu, SKU, giá gốc/giảm giá và chính sách nội dung sản phẩm.
- [ ] Quy tắc phí vận chuyển, ngưỡng miễn phí ship, đổi trả và bảo hành.
- [ ] Nhà cung cấp email, dữ liệu địa chỉ và cổng thanh toán (nếu triển khai).
- [ ] Nơi lưu ảnh sản phẩm/banner và giới hạn dung lượng/tệp được chấp nhận.
- [ ] Quyền nhân viên, chính sách lưu nhật ký và dữ liệu được phép xuất Excel.
