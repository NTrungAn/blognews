# ==========================================
# STAGE 1: Build ứng dụng Spring Boot bằng Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Sao chép pom.xml để tải trước các dependencies (tận dụng Docker layer caching)
COPY pom.xml .

# Tải trước các Maven dependencies ở chế độ offline để tăng tốc độ build những lần sau
RUN mvn dependency:go-offline -B

# Sao chép thư mục mã nguồn nguồn (loại trừ các file trong .dockerignore)
COPY src ./src

# Đóng gói ứng dụng thành file JAR (bỏ qua unit tests để rút ngắn thời gian build image)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Chạy ứng dụng Java trong môi trường JRE nhẹ & bảo mật
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Tăng cường bảo mật: Tạo group và user non-root 'spring' để chạy ứng dụng (tránh quyền root)
RUN addgroup -S spring && adduser -S spring -G spring

# Tạo thư mục uploads và cấp quyền cho user 'spring' để ứng dụng có thể lưu trữ file ảnh tải lên
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Sao chép file JAR đã build từ Stage 1 sang Stage 2 và gán quyền sở hữu cho user 'spring'
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Chuyển quyền thực thi sang user non-root 'spring'
USER spring

# Cấu hình biến môi trường để ứng dụng chạy ổn định
ENV PORT=8080
EXPOSE 8080

# Chạy ứng dụng với các tối ưu hóa JVM dành cho Container:
# - UseContainerSupport: Tự động phát hiện giới hạn RAM/CPU của Container
# - MaxRAMPercentage: Sử dụng tối đa 75% RAM của Container cho Java Heap (tránh OOMKilled do vượt quá RAM container)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+UseG1GC", "-jar", "app.jar"]
