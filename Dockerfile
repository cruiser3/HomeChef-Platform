# Stage 1: Build the app
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 运行时必需环境变量（docker run -e 提供）：
#   JWT_SECRET、DB_PASSWORD（必需）
#   DB_URL / DB_USERNAME / PORT / AI_CONFIG_ENCRYPTION_KEY（可选，默认值见 application.properties）
# 需持久化/挂载的目录：
#   /app/uploads       上传图片与系统菜谱封面（运行时派生，建议挂载卷持久化）
#   /app/HowToCook     美食广场菜谱数据源（可选；缺失则首次导入自动跳过）
VOLUME ["/app/uploads", "/app/HowToCook"]

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]