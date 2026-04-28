# BƯỚC 1: Build toàn bộ dự án
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# BƯỚC 2: Chạy
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy file JAR đã build xong
COPY --from=build /app/target/*.jar app.jar

# CÂU LỆNH QUAN TRỌNG NHẤT:
# Chạy trực tiếp Class Server, bỏ qua Main Class của JavaFX trong pom.xml
ENTRYPOINT ["java", "-cp", "app.jar", "com.auction.server.ServerLauncher"]