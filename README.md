# ☕ Blog News - Backend (Spring Boot + PostgreSQL)

Đây là mã nguồn phần **Backend** của ứng dụng tin tức và blog **Blog News**. Dự án sử dụng framework Spring Boot (Java 21) để xây dựng hệ thống RESTful API, PostgreSQL làm cơ sở dữ liệu chính, Flyway để tự động cập nhật cấu trúc database và tích hợp Google Gemini API để tự động tóm tắt bài viết bằng trí tuệ nhân tạo (AI).

Để ứng dụng hoạt động đầy đủ, bạn cần chạy song song với phần **Frontend**.

🔗 **Link Repository Frontend:** [Link Repo Frontend](https://github.com/your-username/your-frontend-repo-name) *(Vui lòng thay thế bằng link thực tế của bạn)*

---

## 🛠️ Yêu Cầu Hệ Thống (Prerequisites)
Đảm bảo máy tính của bạn đã cài đặt các công cụ sau:
*   **Java Development Kit (JDK) 21** trở lên (khuyên dùng Eclipse Temurin hoặc OpenJDK).
*   **PostgreSQL 16** (đang chạy cục bộ trên máy host).
*   **Maven** (dự án đã tích hợp sẵn Maven Wrapper `./mvnw` hoặc `mvnw.cmd`, nên bạn không bắt buộc phải cài đặt Maven riêng).

---

## 🔑 Hướng Dẫn Cấu Hinh Database & Biến Môi Trường

### Bước 1: Khởi tạo Database trong PostgreSQL
1. Mở công cụ quản lý cơ sở dữ liệu của bạn (pgAdmin, DBeaver, or command line).
2. Kết nối tới PostgreSQL Server trên cổng mặc định `5432`.
3. Tạo một database trống có tên:
   ```sql
   CREATE DATABASE blogsystemdb;
   ```

### Bước 2: Cấu hình biến môi trường local
Spring Boot sẽ tự động nạp cấu hình bổ sung từ file `application-local.properties` (nếu có) để đè lên các cấu hình mặc định mà không ghi đè lên file cấu hình chung của hệ thống.

1. Di chuyển vào thư mục cấu hình:
   ```bash
   cd blogsystem/src/main/resources/
   ```
2. Tạo bản sao từ file ví dụ:
   *   **Windows (PowerShell):**
       ```powershell
       Copy-Item application-local.properties.example application-local.properties
       ```
   *   **Linux / macOS / Git Bash:**
       ```bash
       cp application-local.properties.example application-local.properties
       ```
3. Mở file [application-local.properties](file:///d:/blognews/blogsystem/src/main/resources/application-local.properties) mới tạo và cập nhật các thông số thực tế của bạn:
   ```properties
   # Cấu hình Database máy cá nhân của bạn
   spring.datasource.username=your_db_username      # Tên đăng nhập postgres của bạn
   spring.datasource.password=your_db_password      # Mật khẩu postgres của bạn

   # Khóa API Google Gemini để sử dụng Trợ lý Tóm tắt AI
   GEMINI_API_KEY=AIzaSyYourGeminiApiKeyHere        # Khóa API của bạn (để trống nếu không dùng AI)
   ```

---

## 🚀 Hướng Dẫn Khởi Chạy

### Cách 1: Chạy trực tiếp trên máy Host (Local Development)

1. Di chuyển vào thư mục gốc của backend (`blogsystem/`):
   ```bash
   cd blogsystem
   ```
2. Khởi chạy ứng dụng bằng Maven Wrapper:
   *   **Windows (PowerShell / CMD):**
       ```powershell
       ./mvnw.cmd spring-boot:run
       ```
   *   **Linux / macOS / Git Bash:**
       ```bash
       ./mvnw spring-boot:run
       ```
3. Khi khởi động thành công, ứng dụng sẽ chạy trên cổng `8080`.
   *   Flyway sẽ tự động chạy các file script SQL migration trong thư mục `src/main/resources/db/migration` để tạo bảng và dữ liệu mẫu (seed data) nếu cơ sở dữ liệu trống.
   *   Đường dẫn kiểm tra API: `http://localhost:8080/api/posts`

### Cách 2: Đóng gói và chạy bằng Docker

Nếu bạn muốn đóng gói ứng dụng Backend thành container:

1. Build image Docker cho Backend:
   ```bash
   docker build -t blog-backend .
   ```
2. Khởi chạy container (lưu ý truyền đúng các biến môi trường kết nối database trên máy host hoặc Docker network):
   ```bash
   docker run -d -p 8080:8080 \
     -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/blogsystemdb \
     -e SPRING_DATASOURCE_USERNAME=your_db_username \
     -e SPRING_DATASOURCE_PASSWORD=your_db_password \
     -e GEMINI_API_KEY=your_gemini_key \
     --name blog-backend-app blog-backend
   ```
   *(Tham số `host.docker.internal` giúp container kết nối ngược về PostgreSQL đang chạy ở máy Host)*

---

## 🛡️ Lưu ý Bảo mật
*   File `application-local.properties` chứa các thông tin nhạy cảm về cơ sở dữ liệu và khóa API của bạn. File này đã được cấu hình trong `.gitignore` để không bao giờ bị đẩy lên các kho lưu trữ công khai như GitHub.
