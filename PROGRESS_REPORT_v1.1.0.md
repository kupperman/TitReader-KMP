# 📑 BÁO CÁO TIẾN TRÌNH DỰ ÁN TITREADER-KMP (v1.1.0)
> **Ngày cập nhật:** 20/08/2026  
> **Phiên bản hiện tại:** `v1.1.0` (Build Signed & Debug APK)  
> **Kiến trúc:** Kotlin Multiplatform (KMP) + Jetpack Compose + Ktor + Ksoup + Coil 3

---

## 📌 1. TỔNG QUAN HỆ THỐNG & NHỮNG THAY ĐỔI LỚN

Dự án **TitReader-KMP** đã hoàn thành bước nhảy vọt từ một ứng dụng demo cơ bản thành một ứng dụng đọc truyện đa nền tảng hoàn chỉnh, tích hợp bộ tính năng cốt lõi theo phong cách **Kotatsu** và hệ thống bóc tách dữ liệu chống chặn mạnh mẽ.

```
TitReader-KMP
├── 📦 content-parser-core   --> Core interface, Ksoup parser, True-Race Engine
├── 📖 novel-parsers          --> Parsers truyện chữ (Truyện Full, Truyện Dịch, Truyện Hoàn)
├── 🎨 manga-parsers          --> Parsers truyện tranh (Ổ Truyện, FoxTruyen, TruyệnQQ, NetTruyen)
├── 🧠 shared                 --> Repository, Storage Layer, Models (Library, History, Settings)
└── 📱 androidApp             --> Giao diện Jetpack Compose (Theme, Screens, Reader, Navigation)
```

---

## 🌐 2. HỆ THỐNG NGUỒN DỮ LIỆU THỰC TẾ (DATA SOURCES)

Toàn bộ các nguồn dữ liệu đều được kiểm tra thực tế bằng request trực tiếp từ mạng di động, loại bỏ các web đã chết/bị nhà mạng chặn:

### 📖 Nguồn Truyện Chữ (Novel - 3 nguồn sống 100%)
| ID | Tên hiển thị | Domain gốc | Đặc điểm kỹ thuật |
| :--- | :--- | :--- | :--- |
| `TRUYENFULL` | **Truyện Full** | `https://truyenfull.live` | Kho truyện lớn nhất VN, phân trang chuẩn, đủ thể loại |
| `TRUYENDICH` | **Truyện Dịch** | `https://truyendich.vn` | Tốc độ cực nhanh, chuyên Tiên Hiệp/Kiếm Hiệp, >10.000 đầu truyện |
| `TRUYENHOAN` | **Truyện Hoàn** | `https://truyenhoan.com` | Chuyên truyện đã hoàn thành, load chương tức thì, ảnh bìa CDN |

> **Thanh lọc nguồn cũ:** Đã loại bỏ hoàn toàn `DTruyen` (`dtruyen.com.vn`), `TangThuVien` (`truyen.tangthuvien.vn`), và `TruyenChu` (`truyenchu.net`) do bị timeout / chặn IP di động.

---

### 🎨 Nguồn Truyện Tranh (Manga - 4 nguồn sống 100%)
| ID | Tên hiển thị | Domain gốc | Đặc điểm kỹ thuật |
| :--- | :--- | :--- | :--- |
| `OTRUYEN` | **Ổ Truyện** | `https://otruyenapi.com` | Chuẩn REST API JSON, CDN ảnh tốc độ cao, ổn định tuyệt đối |
| `FOXTRUYEN` | **FoxTruyen** | `https://foxtruyen2.com` | Webtoon & Manhwa phong phú, đã cập nhật live paths & selector |
| `TRUYENQQ` | **TruyệnQQ** | `https://truyenqqko.com` | Kho Manga/Manhua khổng lồ, bóc tách ảnh sắc nét |
| `NETTRUYEN` | **NetTruyen** | `https://nettruyenx.net` | Cập nhật nhanh nhất, hỗ trợ tải ảnh qua Referer routing |

---

## 🌟 3. BỘ 4 TÍNH NĂNG ĐỘT PHÁ CHUẨN KOTATSU (1, 2, 8, 9)

### 📚 Tính Năng 1: Tủ Sách Cá Nhân (Favorites & Categories)
- **Thanh Bottom Bar 3 tab:** Khám Phá, Tủ Sách, Lịch Sử.
- **Nút Yêu Thích 1 chạm (Favorite):** Đặt ngay trên thanh tiêu đề `DetailsScreen`.
- **Phân loại danh mục theo chip:**
  - *Tất cả*, *Đang đọc*, *Yêu thích*, *Đã hoàn thành*, *Kế hoạch đọc*.
- **Lưu trữ vĩnh viễn:** Lưu cục bộ qua `AndroidSharedPreferencesDriver` với JSON serialization, không mất khi tắt app.

### 🕒 Tính Năng 2: Lịch Sử Đọc & Ghi Nhớ Tiến Độ
- **Tự động lưu lịch sử:** Tự động ghi nhận ngay khi mở chương truyện chữ hoặc truyện tranh.
- **Nút "Đọc tiếp [Tên chương]" tức thì:** Xuất hiện nổi bật tại màn hình Chi tiết và màn hình Lịch Sử.
- **Đánh dấu chương đã đọc:** Danh sách chương hiển thị icon Checkmark (`✔`) và làm mờ các chương đã đọc qua.
- **Quản lý lịch sử:** Cho phép xem danh sách đọc gần đây kèm chức năng xoá toàn bộ lịch sử khi cần.

### 🖼️ Tính Năng 8: Đa Chế Độ Đọc Truyện Tranh (Manga Reader)
- Bấm vào màn hình $\rightarrow$ Icon **Cài đặt ⚙️** để mở BottomSheet điều khiển:
  1. **Webtoon (Cuộn dọc vô tận):** Tự động nối liền các trang ảnh dọc mượt mà.
  2. **Lật trang ngang LTR (Trái $\rightarrow$ Phải):** Chế độ đọc Comics / Manhwa theo từng trang.
  3. **Lật trang ngang RTL (Phải $\rightarrow$ Trái):** Chế độ đọc chuẩn Manga Nhật Bản.
- **Badge số trang nổi:** Hiển thị vị trí trang hiện tại (`Trang X / Y`).

### 📖 Tính Năng 9: Tùy Biến Sâu Trình Đọc Truyện Chữ (Novel Reader)
- **4 Theme màu chuyên dụng:**
  - *Sáng (Light):* Nền kem `#FAF6EE`, chữ tối `#1E1914` chống mỏi mắt.
  - *Giấy ngà (Sepia):* Nền vàng ấm `#EEE4CC` êm dịu, chuẩn phong cách sách giấy.
  - *Tối (Slate):* Nền xanh xám đậm `#1E293B` đọc ban đêm.
  - *AMOLED (Đen sâu):* Nền `#000000` tiết kiệm pin tối đa cho màn hình OLED.
- **Tùy chỉnh linh hoạt:**
  - Đổi Font chữ: *Mặc định*, *Sách in (Serif)*, *Máy tính (Monospace)*, *Viết tay*.
  - Cỡ chữ từ **14sp đến 30sp**.
  - Khoảng cách dãn dòng từ **1.2x đến 2.2x**.

---

## 🔧 4. CÁC LỖI KỸ THUẬT QUAN TRỌNG ĐÃ XỬ LÝ TRIỆT ĐỂ

1. **Vá lỗi ảnh bìa bị chặn `403 Forbidden`:**
   - *Nguyên nhân:* CDN của NetTruyen (`kptackpte.com`) và TruyệnQQ (`hinhhinh.com`) chặn request nếu thiếu hoặc sai header `Referer`.
   - *Giải pháp:* Tích hợp cơ chế **Domain-Aware Referer Routing** trong OkHttpClient của `MainActivity`, tự động ánh xạ đúng `Referer` theo từng máy chủ ảnh.

2. **Khắc phục lỗi chữ cái Placeholder đè lên ảnh thật:**
   - Sắp xếp lại thứ tự vẽ trong Jetpack Compose `Box`: Chữ cái nằm ở lớp nền, `AsyncImage` phủ lên trên khi tải xong $\rightarrow$ Ảnh hiện sắc nét, không bị giật lag.

3. **Sửa lỗi FoxTruyen bị Timeout 8000ms:**
   - Thanh lọc domain chết `foxtruyen.com`, cập nhật chuẩn live URL: `/truyen-moi-cap-nhat.html`, `/top-tuan.html`, `/top-binh-chon.html`.

4. **Chuẩn hóa Keystore & Dọn dẹp Release APK:**
   - Keystore an toàn tuyệt đối tại: `D:\DOWNLOAD BACKUP\TitReader-keystore\titreader-release.jks`.
   - Thư mục phát hành gọn gàng, định danh rõ ràng theo phiên bản: `TitReader-v1.1.0-debug.apk` và `TitReader-v1.1.0-release.apk`.

---

## 📦 5. THÔNG TIN FILE CÀI ĐẶT & PHÁT HÀNH

| File APK | Vị trí lưu trữ | Dung lượng | Mục đích sử dụng |
| :--- | :--- | :---: | :--- |
| **`TitReader-v1.1.0-debug.apk`** | `D:\TitReader-KMP\releases-apk\` | **18.9 MB** | 🚀 Cài đặt trực tiếp vào điện thoại để test |
| **`TitReader-v1.1.0-release.apk`** | `D:\TitReader-KMP\releases-apk\` | **13.2 MB** | 🔐 Bản Release đã ký Keystore chuẩn |

- **Git Repository:** `https://github.com/kupperman/TitReader-KMP.git`
- **Branch chính:** `main` (Latest commit: `c284743`)

---

## 🧭 6. LỘ TRÌNH ĐỀ XUẤT CÁC BƯỚC TIẾP THEO (ROADMAP)

Nếu tiếp tục triển khai các tính năng Kotatsu còn lại:
1. **Tính năng 3 (Tải offline / Download Manager):** Tải trước chương truyện tranh và truyện chữ để đọc khi không có mạng.
2. **Tính năng 4 (Tự động kiểm tra chương mới & Thông báo Notification):** Background worker kiểm tra truyện trong Tủ Sách.
3. **Tính năng 5 (Khóa bảo mật ứng dụng - App Lock):** Khóa app bằng PIN hoặc Vân tay / Biometric.
4. **Tính năng 6 (Sao lưu & Khôi phục dữ liệu - Backup & Restore):** Xuất / nhập file JSON cấu hình Tủ Sách và Lịch Sử.