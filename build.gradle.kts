plugins {
    java
    id("org.springframework.boot") version "3.1.8" // 조금 더 안정적인 버전으로
    id("io.spring.dependency-management") version "1.1.3"
}

group = "com.alm"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // SDK 25를 쓰더라도 17로 맞추는 게 안전합니다.
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}