# Chức năng project DA_JAVA

Tài liệu này mô tả vai trò của các file chính trong project, các hàm quan trọng, thư viện đang dùng, và những file/đoạn code có vẻ không còn được gọi trong luồng hiện tại.

## 1. Tổng quan

Project là ứng dụng quản lý quán cà phê bằng Spring Boot MVC:

- Khách hàng đăng nhập/đăng ký, xem menu, chọn sản phẩm, tạo giỏ hàng, chọn hình thức nhận hàng và thanh toán.
- Nhân viên xử lý đơn tại quầy, thanh toán hoặc hủy đơn.
- Admin quản lý sản phẩm, nhân viên, đơn hàng và xem thống kê.
- Dữ liệu lưu trong SQL Server, giao diện dùng Thymeleaf template.

## 2. Thư viện chính

File `pom.xml` khai báo các thư viện:

- `spring-boot-starter-web`: tạo web server, controller, route HTTP, session, SSE.
- `spring-boot-starter-thymeleaf`: render các file HTML trong `src/main/resources/templates`.
- `spring-boot-starter-validation`: hỗ trợ validation bean/request nếu cần mở rộng.
- `mssql-jdbc`: driver kết nối SQL Server.
- `zxing-core`, `zxing-javase`: tạo QR code thanh toán.
- `spring-boot-starter-test`: chạy test Spring Boot/JUnit.

## 3. File cấu hình và khởi động

### `src/main/java/com/huit/da_java/App.java`

File khởi động ứng dụng.

- `main(String[] args)`: gọi `SpringApplication.run(...)` để chạy Spring Boot.
- Annotation `@SpringBootApplication`: bật auto configuration, component scan và cấu hình Spring.

### `src/main/resources/application.properties`

Cấu hình runtime:

- `spring.application.name`: tên app.
- `server.port=${PORT:8080}`: chạy theo biến môi trường `PORT`, mặc định 8080.
- `spring.thymeleaf.cache=false`: tắt cache template khi dev.
- `server.error.whitelabel.enabled=false`: dùng trang lỗi tự tạo thay vì whitelabel error.

### `src/main/resources/db/cafe-management-reset.sql`

Script tạo/reset database, bảng, ràng buộc và dữ liệu mẫu.

## 4. Controller

Controller nhận request từ browser, kiểm tra session/quyền, gọi DAO/service, rồi trả về template hoặc redirect.

### `LoginController.java`

Quản lý đăng nhập/đăng xuất.

- `home()`: chuyển trang gốc `/` về trang đăng nhập nội bộ.
- `login(...)`: xử lý đăng nhập chung theo vai trò.
- `internalLogin(...)`: hiển thị form đăng nhập admin/nhân viên.
- `customerLoginPage(...)`: hiển thị form đăng nhập khách hàng.
- `staffLogin(...)`: đăng nhập admin/nhân viên, lưu `currentUser` vào `HttpSession`.
- `customerLogin(...)`: đăng nhập khách hàng, lưu `currentCustomer` vào `HttpSession`.
- `register(...)`: đăng ký khách hàng mới.
- `logout(...)`: xóa session và quay về trang đăng nhập.

Thư viện/API dùng: Spring MVC (`@Controller`, `@GetMapping`, `@PostMapping`, `Model`, `RedirectAttributes`), `HttpSession`.

### `AdminController.java`

Quản lý khu vực admin.

- `overview(...)`: dashboard admin, thống kê doanh thu, số sản phẩm, nhân viên, đơn hàng gần đây.
- `products(...)`: danh sách sản phẩm để thêm/sửa/xóa.
- `staff(...)`: danh sách nhân viên để thêm/sửa/khóa/mở.
- `pendingOrders(...)`: danh sách đơn đang chờ xử lý.
- `saveProduct(...)`: thêm hoặc cập nhật sản phẩm.
- `deleteProduct(...)`: xóa sản phẩm.
- `cancelOrder(...)`: hủy đơn hàng.
- `saveStaff(...)`: thêm hoặc cập nhật nhân viên.
- `toggleStaff(...)`: khóa/mở tài khoản nhân viên.
- `requireAdmin(...)`: kiểm tra session có phải admin không.
- `populateAdminUser(...)`: đưa thông tin admin hiện tại ra giao diện.
- `addProductReferenceData(...)`: nạp category/trạng thái phục vụ form sản phẩm.

Thư viện/API dùng: Spring MVC, `HttpSession`, `RedirectAttributes`, các DAO.

### `CustomerController.java`

Quản lý luồng khách hàng.

- `menu(...)`: hiển thị menu sản phẩm cho khách hàng.
- `order(...)`: nhận giỏ hàng từ form, tạo đơn hàng.
- `payment(...)`: hiển thị trang thanh toán cho đơn vừa tạo hoặc đơn hiện tại.
- `choosePaymentMethod(...)`: lưu phương thức thanh toán, hình thức nhận hàng và địa chỉ giao hàng nếu có.
- `paymentQr(...)`: tạo ảnh QR thanh toán cho đơn.
- `confirmPayment(...)`: xác nhận thanh toán, cập nhật trạng thái đơn.
- `buildItems(...)`: chuyển dữ liệu `productIds`/`quantities` thành danh sách item hợp lệ.
- `isBlank(...)`: kiểm tra chuỗi rỗng.
- `getCurrentCustomerOrder(...)`: lấy đơn đang thanh toán từ session hoặc database.
- `buildPaymentContent(...)`: tạo nội dung chuyển khoản.
- `normalizeOrderType(...)`: chuẩn hóa hình thức nhận hàng.
- `normalizePaymentMethod(...)`: chuẩn hóa phương thức thanh toán.
- `normalizeDeliveryAddress(...)`: xử lý địa chỉ giao hàng, bắt buộc khi giao tận nhà.
- `buildQrPayload(...)`: tạo payload QR từ số tiền và nội dung.
- `hasRequestedPaymentMethod(...)`: kiểm tra khách đã chọn phương thức thanh toán chưa.
- `findProducts(...)`: lấy thông tin sản phẩm theo id để hiển thị giỏ/đơn.

Thư viện/API dùng: Spring MVC, `HttpSession`, `RedirectAttributes`, `ResponseEntity`, `MediaType`, DAO và service tạo QR.

### `StaffController.java`

Quản lý màn hình nhân viên.

- `pos(...)`: hiển thị màn hình xử lý đơn cho nhân viên.
- `orderStream(...)`: mở kênh SSE nhận thông báo đơn mới.
- `checkout(...)`: nhân viên xác nhận thanh toán đơn.
- `cancel(...)`: nhân viên hủy đơn.
- `requireStaff(...)`: kiểm tra session có phải nhân viên hoặc admin không.

Thư viện/API dùng: Spring MVC, `HttpSession`, `RedirectAttributes`, `SseEmitter`.

## 5. Service

Service xử lý nghiệp vụ phụ trợ nằm ngoài controller/DAO.

### `QrCodeService.java`

Tạo ảnh QR PNG.

- `generatePng(String content, int size)`: dùng ZXing tạo ma trận QR từ nội dung, render thành PNG byte array.

Thư viện dùng:

- `com.google.zxing.BarcodeFormat`
- `com.google.zxing.MultiFormatWriter`
- `com.google.zxing.client.j2se.MatrixToImageWriter`
- `com.google.zxing.common.BitMatrix`
- Java `ByteArrayOutputStream`

### `VietQrPayloadService.java`

Tạo payload VietQR/MoMo chuyển khoản.

- `buildMomoTransferPayload(BigDecimal amount, String content)`: tạo chuỗi QR đã gắn số tiền, nội dung và CRC.
- `buildAdditionalData(String content)`: tạo phần dữ liệu bổ sung cho nội dung thanh toán.
- `stripCrc(String payload)`: bỏ phần CRC cũ khỏi payload mẫu.
- `parse(String payload)`: parse payload TLV thành danh sách record.
- `encode(List<Tlv> records)`: encode danh sách TLV thành chuỗi.
- `find(...)`, `set(...)`, `setAfter(...)`, `remove(...)`: tìm, thêm, sửa, xóa TLV record.
- `crc16(String value)`: tính CRC16 theo chuẩn QR.
- `Tlv`: record nội bộ lưu `id` và `value`.

Thư viện dùng: Java core (`BigDecimal`, `List`, `ArrayList`, `Locale`).

### `OrderNotificationService.java`

Gửi thông báo đơn mới cho màn hình nhân viên bằng Server-Sent Events.

- `subscribe()`: tạo `SseEmitter` mới cho client.
- `notifyNewOrder(OrderNotification notification)`: gửi thông báo đơn mới cho tất cả subscriber.
- `getSubscriberCount()`: đếm số client đang lắng nghe.
- `createEmitter(String id)`: tạo emitter và đăng ký cleanup khi lỗi/timeout/complete.

Thư viện dùng:

- `SseEmitter` của Spring MVC.
- `ConcurrentHashMap` để quản lý subscriber an toàn khi nhiều request.

### `OrderNotification.java`

Record dữ liệu thông báo đơn mới.

- Gồm các trường: mã đơn, tên khách, tổng tiền, loại đơn, địa chỉ giao hàng, thời gian đặt.

## 6. DAO

DAO làm việc trực tiếp với SQL Server bằng JDBC.

### `DatabaseConnection.java`

Tạo kết nối database.

- `getConnection()`: đọc cấu hình database từ biến môi trường hoặc giá trị mặc định, trả về `Connection`.
- `firstNonBlank(...)`: lấy giá trị đầu tiên không rỗng trong danh sách cấu hình.

Thư viện dùng: JDBC `Connection`, `DriverManager`, `SQLException`.

### `CustomerDAO.java`

Quản lý khách hàng.

- `findByUsername(String username)`: tìm khách hàng theo username.
- `getById(int id)`: lấy khách hàng theo id.
- `insert(Customer customer)`: thêm khách hàng mới.
- `update(Customer customer)`: cập nhật thông tin khách hàng.
- `mapCustomer(ResultSet rs)`: chuyển dữ liệu SQL thành model `Customer`.

Ghi chú: `getById` và `update` hiện chưa thấy controller gọi trực tiếp.

### `UserDAO.java`

Quản lý tài khoản admin/nhân viên.

- `findByUsername(String username)`: tìm user theo username.
- `findStaffUsers()`: lấy danh sách nhân viên.
- `countStaffUsers()`: đếm nhân viên.
- `insertStaff(User user)`: thêm nhân viên.
- `updateStaff(User user)`: cập nhật nhân viên.
- `toggleStaffStatus(int id, boolean active)`: đổi trạng thái nhân viên theo tham số boolean.
- `toggleActive(int id)`: đảo trạng thái đang hoạt động.
- `mapUser(ResultSet rs)`: chuyển dữ liệu SQL thành model `User`.

Ghi chú: `toggleStaffStatus` hiện có vẻ chưa được gọi; controller đang dùng `toggleActive`.

### `ProductDAO.java`

Quản lý sản phẩm và danh mục.

- Lấy danh sách sản phẩm theo trạng thái/category.
- Lấy sản phẩm theo id.
- Thêm, sửa, xóa sản phẩm.
- Lấy danh sách category.
- Đếm sản phẩm.
- Chuẩn hóa ảnh mặc định cho sản phẩm.
- Map dữ liệu SQL thành `Product` và `Category`.

Thư viện dùng: JDBC và Java collection.

### `OrderDAO.java`

Quản lý đơn hàng, chi tiết đơn, thống kê và trạng thái thanh toán/hủy.

Các nhóm chức năng chính:

- Tạo đơn:
  - `createCustomerOrder(...)`: tạo đơn từ giỏ hàng của khách.
  - `createOrder(...)`: hàm tạo đơn kiểu cũ/legacy.
  - `createAndPayStaffOrder(...)`: tạo và thanh toán đơn tại quầy kiểu cũ/legacy.
  - `addProductToOrder(...)`: thêm sản phẩm vào chi tiết đơn kiểu cũ/legacy.

- Thanh toán/hủy đơn:
  - `payOrder(int orderId, String paymentMethod)`: cập nhật đơn sang đã thanh toán.
  - `markPaid(...)`: xác nhận đơn đã thanh toán.
  - `cancelOrder(int orderId)`: hủy đơn.
  - `ensureOrderCancellationSupport(...)`: đảm bảo database cho phép trạng thái `cancelled`.
  - `quoteSqlServerIdentifier(...)`: escape tên constraint SQL Server khi cần drop/tạo lại.

- Lấy dữ liệu đơn:
  - Lấy đơn chờ xử lý.
  - Lấy đơn theo id.
  - Lấy đơn gần đây.
  - Lấy chi tiết sản phẩm trong đơn.

- Thống kê:
  - Tính doanh thu hôm nay.
  - Đếm đơn hôm nay.
  - Lấy sản phẩm bán chạy.
  - Đếm sản phẩm.

- Mapping:
  - Chuyển `ResultSet` thành `Order`, `OrderDetail`, `ProductSalesItem`, `StatisticItem`.

Ghi chú: một số hàm legacy như `createOrder`, `createAndPayStaffOrder`, `addProductToOrder`, `payOrder`, `getTopSellingProducts`, `countProducts` hiện chưa thấy controller gọi trực tiếp trong luồng mới, nhưng có thể giữ nếu còn định phát triển POS/bàn.

### `TableDAO.java`

Quản lý bàn cà phê.

- Lấy danh sách bàn.
- Lấy bàn theo id.
- Cập nhật trạng thái bàn.
- Map dữ liệu SQL thành `CafeTable`.

Ghi chú: hiện chưa thấy controller/service nào gọi `TableDAO`, nên file này đang là ứng viên chưa dùng trong luồng hiện tại.

## 7. Model

Model là lớp dữ liệu dùng giữa DAO, controller và template.

### `User.java`

Đại diện tài khoản admin/nhân viên.

- Thuộc tính thường có: id, username, password, full name, role, active.
- Dùng cho đăng nhập nội bộ, phân quyền admin/staff và quản lý nhân viên.

### `Customer.java`

Đại diện khách hàng.

- Lưu thông tin đăng nhập, họ tên, số điện thoại, email/địa chỉ nếu có.
- Dùng trong đăng nhập khách hàng và tạo đơn.

### `Category.java`

Đại diện danh mục sản phẩm.

- Dùng để lọc/hiển thị sản phẩm theo nhóm.

### `Product.java`

Đại diện sản phẩm.

- Lưu tên, giá, ảnh, danh mục, mô tả, trạng thái hoạt động.
- Dùng ở menu khách hàng, POS nhân viên và quản lý sản phẩm admin.

### `CafeTable.java`

Đại diện bàn trong quán.

- Dùng chung với `TableDAO`.
- Hiện tại có vẻ chưa được luồng web gọi tới.

### `Order.java`

Đại diện đơn hàng.

- Lưu mã đơn, khách hàng, tổng tiền, trạng thái, phương thức thanh toán, hình thức nhận hàng, địa chỉ giao hàng và thời gian đặt.
- `getOrderTimeText()`: format thời gian đặt hàng dạng dễ đọc để hiển thị trên giao diện.

### `OrderDetail.java`

Đại diện từng dòng sản phẩm trong đơn.

- Lưu sản phẩm, số lượng, đơn giá, thành tiền.

### `ProductSalesItem.java`

Dữ liệu thống kê sản phẩm bán chạy.

- Lưu tên sản phẩm, số lượng bán và doanh thu.

### `StatisticItem.java`

Dữ liệu thống kê tổng quát.

- Lưu label và value để hiển thị trên dashboard.

## 8. Template HTML

Các file trong `src/main/resources/templates` là giao diện Thymeleaf.

- `login.html`: trang đăng nhập chung hoặc trang chuyển hướng đăng nhập.
- `login-internal.html`: đăng nhập admin/nhân viên.
- `login-customer.html`: đăng nhập/đăng ký khách hàng.
- `admin.html`: dashboard admin.
- `admin-products.html`: quản lý sản phẩm.
- `admin-staff.html`: quản lý nhân viên.
- `admin-orders.html`: quản lý đơn hàng.
- `staff-pos.html`: màn hình nhân viên xử lý đơn.
- `customer-menu.html`: menu, chọn số lượng, chọn sản phẩm vào giỏ hàng.
- `customer-payment.html`: trang thanh toán, chọn phương thức thanh toán và hình thức nhận hàng.
- `error.html`: trang lỗi custom của Spring Boot.

## 9. Static resource

### `src/main/resources/static/css/app.css`

CSS dùng chung cho toàn bộ giao diện.

### `src/main/resources/static/images`

Ảnh giao diện và ảnh sản phẩm.

- `images/ui/cafe-pattern.svg`: background/pattern dùng trong CSS.
- `images/products/*.jpg`: ảnh sản phẩm mặc định, được database seed và `ProductDAO` tham chiếu.

Không nên xóa các ảnh sản phẩm hiện có nếu dữ liệu mẫu còn dùng đường dẫn tương ứng.

## 10. File có vẻ chưa dùng hoặc có thể dọn

Danh sách này dựa trên việc dò tham chiếu trong source hiện tại. Trước khi xóa thật nên cân nhắc có còn cần cho tính năng tương lai không.

### Có vẻ không được gọi trong luồng hiện tại

- `src/main/java/com/huit/da_java/utils/SessionManager.java`
  - Singleton quản lý session kiểu cũ.
  - Web hiện tại dùng `HttpSession`, chưa thấy file nào gọi `SessionManager`.

- `src/main/java/com/huit/da_java/utils/PermissionChecker.java`
  - Hàm kiểm tra quyền theo role.
  - Controller hiện tự kiểm tra bằng `requireAdmin(...)` và `requireStaff(...)`, chưa thấy gọi class này.

- `src/main/java/com/huit/da_java/dao/TableDAO.java`
  - DAO quản lý bàn.
  - Chưa thấy controller/service gọi tới trong luồng hiện tại.

- `src/main/java/com/huit/da_java/model/CafeTable.java`
  - Model bàn.
  - Chỉ còn ý nghĩa nếu giữ `TableDAO` hoặc phát triển lại tính năng bàn.

### Hàm public có vẻ chưa được gọi trực tiếp

- `CustomerDAO.getById(...)`
- `CustomerDAO.update(...)`
- `UserDAO.toggleStaffStatus(...)`
- Một số hàm legacy trong `OrderDAO`: `createOrder(...)`, `createAndPayStaffOrder(...)`, `addProductToOrder(...)`, `payOrder(...)`, `getTopSellingProducts(...)`, `countProducts(...)`

Các hàm này chưa nhất thiết là thừa hoàn toàn, vì có thể được giữ để phát triển lại luồng POS/bàn hoặc thống kê sau này.

### File sinh ra khi build hoặc file công cụ

- `target/`: thư mục build Maven, có thể xóa an toàn, Maven sẽ tạo lại.
- `.github/java-upgrade/...`: log/hook phục vụ công cụ nâng cấp Java, không phải runtime app.
- `.vscode/settings.json`: cấu hình IDE.
- `nbactions.xml`: cấu hình NetBeans.
- `RUNNING.md`, `docs/SDS.md`: tài liệu, không phải runtime app.

## 11. Khuyến nghị dọn project

- Có thể xóa `target/` bất cứ lúc nào nếu muốn nhẹ project.
- Có thể giữ `.vscode/settings.json`, `nbactions.xml` nếu còn dùng VS Code/NetBeans; không ảnh hưởng app.
- Chỉ nên xóa `SessionManager`, `PermissionChecker`, `TableDAO`, `CafeTable` nếu chắc chắn không phát triển tính năng bàn/session kiểu cũ nữa.
- Không nên xóa template, CSS, ảnh sản phẩm hoặc `error.html` vì chúng đang phục vụ giao diện hiện tại.

