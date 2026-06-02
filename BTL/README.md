# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

## 1. Mô tả bài toán và phạm vi hệ thống
Hệ thống là một ứng dụng desktop (Client-Server) cho phép người dùng tham gia các phiên đấu giá trực tuyến theo thời gian thực. Hệ thống hỗ trợ đa người dùng, đảm bảo tính nhất quán của dữ liệu giá thầu và cung cấp giao diện trực quan cho cả người mua, người bán và quản trị viên.

**Phạm vi hệ thống:**
- **Người dùng:** Đăng ký/Đăng nhập, quản lý số dư (nạp/rút tiền), đăng bán sản phẩm (cần admin duyệt), tham gia đấu giá trực tiếp, đấu giá tự động (Bot), theo dõi danh sách sản phẩm đã thắng.
- **Quản trị viên (Admin):** Kiểm soát toàn bộ hệ thống, duyệt/từ chối yêu cầu đăng bán, quản lý danh sách người dùng (khóa/mở khóa), giám sát các phiên đấu giá đang diễn ra.
- **Hệ thống:** Xử lý đấu giá theo thời gian thực qua WebSocket, tự động kết thúc phiên đấu giá và cập nhật trạng thái sản phẩm, tích hợp lưu trữ ảnh trên Cloud.

## 2. Công nghệ và Yêu cầu cài đặt
- **Ngôn ngữ lập trình:** Java 17
- **Giao diện người dùng:** JavaFX 17
- **Cơ sở dữ liệu:** PostgreSQL (Lưu trữ thông tin người dùng, sản phẩm, phiên đấu giá)
- **Giao tiếp mạng:** WebSockets (Java-WebSocket) - Đảm bảo cập nhật thời gian thực.
- **Định dạng dữ liệu:** JSON (Sử dụng Gson & Jackson)
- **Lưu trữ hình ảnh:** Cloudinary API
- **Quản lý dự án & Thư viện:** Maven
- **Bảo mật:** jBCrypt (Mã hóa mật khẩu)

### Yêu cầu môi trường:
- **JDK:** Phiên bản 17 hoặc cao hơn.
- **Maven:** Đã được cài đặt và cấu hình biến môi trường.
- **Database:** PostgreSQL đang chạy (Mặc định kết nối tới database `auction_db` tại `localhost:5432`).

## 3. Cấu trúc thư mục chính
```text
BTL/
├── src/main/java/com/auction/
│   ├── client/         # Logic phía Client (JavaFX Controllers, UI, Network Client)
│   │   ├── controller/ # Xử lý sự kiện trên giao diện (Login, Home, Shop, Admin...)
│   │   ├── network/    # Giao tiếp WebSocket phía Client
│   │   └── view/       # (Resources) File .fxml và .css
│   ├── server/         # Logic phía Server
│   │   ├── dao/        # Truy xuất cơ sở dữ liệu (UserDao, ProductDao, AuctionDao)
│   │   ├── handler/    # Xử lý các loại thông báo từ Client (Bidding, Bank, Shop...)
│   │   ├── service/    # Quản lý logic đấu giá và kết nối
│   │   └── ServerLauncher.java # Điểm khởi đầu của Server
│   ├── common/         # Model dùng chung (User, Product, Auction) và Utils
│   └── protocol/       # Định nghĩa cấu trúc gói tin Request/Response
├── src/main/resources/ # Tài nguyên hệ thống (Ảnh, FXML, SQL migrations)
└── pom.xml             # File cấu hình Maven
```

## 4. Hướng dẫn chạy chương trình
Cần đảm bảo **Server phải được chạy trước** để Client có thể kết nối.

### Bước 1: Chuẩn bị Cơ sở dữ liệu
1. Mở PostgreSQL (pgAdmin hoặc terminal).
2. Tạo một database mới tên là `auction_db`.
3. Kiểm tra thông tin kết nối trong file `BTL/src/main/java/com/auction/server/db/Db.java` (User/Pass mặc định: `postgres`/`enix`).

### Bước 2: Chạy Server
Mở terminal/command prompt tại thư mục gốc của dự án (`BTL/`) và chạy lệnh tùy theo hệ điều hành của bạn:

#### 🟢 Windows (Command Prompt / PowerShell)
```cmd
mvn exec:java -Dexec.mainClass="com.auction.server.ServerLauncher"
```

#### 🍎 macOS / 🐧 Linux (Terminal)
```bash
mvn exec:java -Dexec.mainClass="com.auction.server.ServerLauncher"
```

### Bước 3: Chạy Client
Mở một terminal mới (vẫn tại thư mục `BTL/`) và chạy lệnh:

#### 🟢 Windows (Command Prompt / PowerShell)
```cmd
mvn javafx:run
```

#### 🍎 macOS / 🐧 Linux (Terminal)
```bash
mvn javafx:run
```

*Lưu ý: Giao diện đăng nhập sẽ hiện ra. Bạn có thể mở nhiều terminal để chạy nhiều Client cùng lúc nhằm thử nghiệm tính năng đấu giá giữa nhiều người dùng.*

## 5. Danh sách chức năng đã hoàn thành

### Hệ thống & Bảo mật
- [x] Đăng ký, Đăng nhập với mật khẩu mã hóa (jBCrypt).
- [x] Đăng xuất và quản lý phiên làm việc.
- [x] Cập nhật ảnh đại diện người dùng lên Cloudinary.

### Quản lý Tài chính (Bank)
- [x] Xem số dư tài khoản.
- [x] Nạp tiền và Rút tiền ảo vào hệ thống.

### Chức năng Đấu giá (Bidding)
- [x] Đấu giá trực tiếp thời gian thực (Real-time bidding).
- [x] Tự động cập nhật giá thầu cao nhất mà không cần tải lại trang.
- [x] Chức năng Đấu giá tự động (Bot Bidding) cho người dùng.
- [x] Xem danh sách các sản phẩm đã đấu giá thắng (Won Auctions).
- [x] Chế độ xem TikTok Mode: Lướt xem nhanh các phiên đấu giá đang diễn ra.

### Quản lý Cửa hàng (Shop)
- [x] Đăng sản phẩm mới lên sàn đấu giá.
- [x] Chỉnh sửa và Xóa sản phẩm.
- [x] Quản lý danh sách sản phẩm trong shop cá nhân.
- [x] Nhập sản phẩm (Import) từ danh sách mẫu.

### Quản trị viên (Admin)
- [x] Duyệt hoặc từ chối yêu cầu đăng bán sản phẩm của người dùng.
- [x] Xem danh sách người dùng đang trực tuyến.
- [x] Khóa tài khoản (Ban) hoặc đưa vào danh sách đen (Blacklist).
- [x] Buộc người dùng đăng xuất từ xa.
- [x] Hủy các phiên đấu giá đang diễn ra nếu có vi phạm.
