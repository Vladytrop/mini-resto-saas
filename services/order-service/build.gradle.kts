plugins {
    id("org.springframework.boot")
    application
}

val kafkaVersion = "3.7.0"
val openapiVersion = "2.6.0"
val resilience4jVersion = "2.1.0"

dependencies {
    implementation(project(":services:shared"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.kafka:kafka_2.13:$kafkaVersion")

    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$openapiVersion")

    runtimeOnly("org.postgresql:postgresql")
}
