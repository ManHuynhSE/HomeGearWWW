# Kế hoạch hoàn thiện và nâng cấp HomeGear

> Checklist triển khai theo thứ tự ưu tiên. Các mục `[x]` là nền tảng đã có theo hiện trạng project; các mục `[ ]` là việc cần làm.  
> Phạm vi MVP hiện tại đã có catalog cơ bản, trang chi tiết sản phẩm, giỏ hàng theo session, đặt hàng/lịch sử đơn và trang quản trị cơ bản. Các mục tương ứng bên dưới là phần hoàn thiện/nâng cấp, không phải xây lại từ đầu.

## P0 — Sửa ngay các điểm làm giao diện trông như dữ liệu demo

- [ ] Rà soát và xóa dữ liệu sản phẩm/danh mục thử nghiệm không phù hợp; thay bằng dữ liệu mẫu thật, nhất quán với ngành hàng HomeGear.
- [ ] Quy định xử lý sản phẩm hết hàng: vẫn hiển thị để khách tham khảo, có nhãn **Hết hàng**, vô hiệu hóa nút thêm giỏ/mua ngay; không cho checkout vượt tồn kho.
- [ ] Chuẩn hóa định dạng tiền Việt trên toàn ứng dụng: `1.890.000 ₫` (dấu chấm phân tách hàng nghìn, ký hiệu `₫`).
- [ ] Thay mã trạng thái đơn tiếng Anh trên giao diện bằng nhãn tiếng Việt: `NEW` → **Mới**, `CONFIRMED` → **Đã xác nhận**, `SHIPPING` → **Đang giao**, `COMPLETED` → **Hoàn tất**, `CANCELLED` → **Đã hủy**; giữ mã nội bộ ổn định.
- [ ] Hoàn thiện footer thay cho footer tối giản hiện tại; đảm bảo thông tin chính sách/liên hệ không phải nội dung placeholder.
- [ ] Kiểm tra dữ liệu thử nghiệm hiện có trong MariaDB và lập cách dọn/thay thế an toàn trước khi áp dụng; không xóa dữ liệu người dùng thật.

## P1 — Hoàn thiện trang chủ và nhận diện cửa hàng

- [ ] Thêm thanh thông báo: **Miễn phí vận chuyển đơn từ 500.000 ₫**; chỉ hiển thị điều kiện khi chính sách phí ship đã được xác định.
- [ ] Thiết kế lại header gồm logo, ô tìm kiếm nổi bật, giỏ hàng có số lượng, tài khoản và điều hướng danh mục.
- [ ] Xây dựng gợi ý tự động cho ô tìm kiếm; hỗ trợ bàn phím, trạng thái không có gợi ý và truy cập trên mobile.
- [ ] Thêm menu danh mục/mega menu trên desktop và menu hamburger trên mobile.
- [ ] Thêm dải danh mục có icon hoặc thẻ, ví dụ: Nhà bếp, Thiết bị thông minh, Máy lạnh.
- [ ] Thêm dải cam kết: giao hàng nhanh, đổi trả 30 ngày, bảo hành chính hãng, thanh toán an toàn; chỉ quảng bá chính sách cửa hàng thực sự áp dụng.
- [ ] Tổ chức khu vực sản phẩm thành các nhóm Bán chạy, Mới về và Khuyến mãi.
- [ ] Bổ sung dữ liệu/hiển thị giá gốc, phần trăm giảm và badge khuyến mãi; không tự tạo giá giảm nếu chưa có dữ liệu chương trình.
- [ ] Thiết kế banner khuyến mãi/flash sale; chỉ bật đồng hồ đếm ngược khi có thời gian bắt đầu/kết thúc được cấu hình.
- [ ] Thêm đánh giá khách hàng và logo thương hiệu sau khi có dữ liệu/nguồn nội dung phù hợp.
- [ ] Thêm đăng ký nhận tin; xác thực email và thông báo rõ việc lưu/ sử dụng địa chỉ email.
- [ ] Hoàn thiện footer 4 cột: giới thiệu, hỗ trợ/chính sách, liên hệ, mạng xã hội và phương thức thanh toán.
- [ ] Thêm nút liên hệ nổi Zalo/Messenger/hotline khi đã cấu hình đúng thông tin liên hệ.

## P2 — Trang danh sách và khám phá sản phẩm

- [x] Duy trì trang catalog, tìm kiếm và trang chi tiết sản phẩm hiện có.
- [ ] Thêm breadcrumb, tổng số kết quả và trạng thái rỗng **Không tìm thấy sản phẩm** kèm gợi ý xóa bộ lọc/đổi từ khóa.
- [ ] Thêm bộ lọc danh mục, khoảng giá và tình trạng còn hàng.
- [ ] Thêm bộ lọc thương hiệu sau khi thương hiệu trở thành dữ liệu có quản lý trong hệ thống.
- [ ] Thêm sắp xếp giá tăng/giảm, mới nhất và bán chạy; xác định nguồn dữ liệu/thứ tự cho tiêu chí bán chạy.
- [ ] Thêm phân trang hoặc **Xem thêm**, giữ lại trạng thái tìm kiếm/bộ lọc khi chuyển trang.
- [ ] Nâng cấp thẻ sản phẩm với ảnh, nhãn Mới/giảm giá, đánh giá sao, giá và nút xem nhanh/yêu thích.
- [ ] Bổ sung ảnh sản phẩm và thuộc tính cần thiết; hiển thị ảnh thay thế khi chưa có ảnh.
- [ ] Tối ưu truy vấn catalog để lọc/sắp xếp/phân trang ở database, tránh tải toàn bộ dữ liệu lên bộ nhớ khi danh mục lớn.

## P3 — Trang chi tiết sản phẩm

- [x] Có template và luồng trang chi tiết sản phẩm cơ bản.
- [ ] Bổ sung gallery ảnh chính và ảnh thu nhỏ; hỗ trợ phóng to/xem ảnh trên mobile.
- [ ] Hiển thị tên, SKU, đánh giá, giá hiện tại, giá gốc và phần trăm giảm khi có dữ liệu tương ứng.
- [ ] Bổ sung chọn số lượng, trạng thái tồn kho chính xác, nút **Thêm vào giỏ** và **Mua ngay**.
- [ ] Vô hiệu hóa thao tác mua khi hết hàng; kiểm tra lại tồn kho ở backend lúc checkout.
- [ ] Tổ chức nội dung thành tab Mô tả, Thông số kỹ thuật, Đánh giá và Chính sách.
- [ ] Hiển thị sản phẩm liên quan, thông tin vận chuyển và bảo hành đã được cửa hàng xác nhận.
- [ ] Cập nhật title, meta description và dữ liệu có cấu trúc Product cho từng sản phẩm.
- [ ] Tạo URL thân thiện theo slug; bảo đảm URL cũ vẫn được xử lý/chuyển hướng nếu đang được sử dụng.

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
