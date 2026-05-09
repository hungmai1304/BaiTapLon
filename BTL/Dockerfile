# --- BƯỚC 1: BUILD ---
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
# Đóng gói toàn bộ project thành file .jar
RUN mvn clean package -DskipTests

# --- BƯỚC 2: RUN ---
# Dùng bản JRE siêu nhẹ, không chứa Maven để tiết kiệm RAM
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
# Copy file .jar từ BƯỚC 1 sang BƯỚC 2
COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

# Chạy trực tiếp bằng Java, cởi bỏ hoàn toàn gánh nặng của Maven
CMD ["java","--add-opens", "-jar", "app.jar"]