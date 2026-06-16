plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val springdocVersion = "2.6.0"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$springdocVersion")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
    }
}