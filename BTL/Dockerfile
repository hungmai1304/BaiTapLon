# BƯỚC 1: Build dự án bằng Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# BƯỚC 2: Chạy ứng dụng (Đã sửa dòng lỗi)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy file JAR từ bước build sang
COPY --from=build /app/target/*.jar app.jar

# Port Render cấp
EXPOSE 8080

# Chạy trực tiếp Class Server đầu não của bạn
ENTRYPOINT ["java", "-cp", "app.jar", "com.auction.server.ServerLauncher"]