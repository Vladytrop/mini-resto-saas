plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.resto.saas"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }  // Для Spring AI milestone
    maven { url = uri("https://repo.spring.io/snapshot") }   // Для Spring AI snapshot
}

// Spring AI BOM —统一管理 всех зависимостей Spring AI
val springAiVersion = "1.0.0-M3"

dependencies {
    implementation(project(":services:shared"))

    // Spring Boot basics
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring AI — ключевой стек для RAG
    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))
    implementation("org.springframework.ai:spring-ai-core")
    implementation("org.springframework.ai:spring-ai-pgvector-store")  // pgvector интеграция
    implementation("org.springframework.ai:spring-ai-tika-document-reader")  // чтение документов
    implementation("org.springframework.ai:spring-ai-markdown-document-reader")

    // Resilience4j — circuit breaker для LLM API
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")  // для @CircuitBreaker

    // БД
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Для OpenAiChatOptions (используем только билдер)
    implementation("org.springframework.ai:spring-ai-openai")

    // Метрики
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Jackson для JSON
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.withType<Test> {
    useJUnitPlatform()
}