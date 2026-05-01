# Sử dụng trực tiếp Maven kèm Java 25 mới nhất
FROM maven:3.9-eclipse-temurin-25
WORKDIR /app

# Copy toàn bộ mã nguồn và file pom.xml vào Docker
COPY . .

# Compile dự án bằng Java 25 (Sẽ không còn lỗi release version 25 not supported nữa)
RUN mvn clean compile -DskipTests

# Port cho Render
EXPOSE 8080

# Chạy trực tiếp qua Maven Exec Plugin
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.auction.server.ServerLauncher"]