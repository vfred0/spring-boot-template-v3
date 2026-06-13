plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.template"
version = "0.1.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

val mapstructVersion = "1.6.3"
val flywayVersion = "10.22.0"
val bucket4jVersion = "8.0.1"
val oauth2OidcSdkVersion = "11.9.1"
val springdocVersion = "2.8.0"
val mockitoVersion = "5.20.0"
val jackson2Version = "2.21.1"
val modelmapperVersion = "3.2.6"

// Configuración personalizada para extraer el javaagent de Mockito en tiempo de ejecución
val mockitoAgent by configurations.creating

dependencies {
    // Jackson Constraint (equivalente a dependencyManagement en Maven)
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-core:$jackson2Version")
    }

    // Web & Validation
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Security & JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("com.nimbusds:oauth2-oidc-sdk:$oauth2OidcSdkVersion")

    // Rate Limiting
    implementation("com.bucket4j:bucket4j-core:$bucket4jVersion")

    // Caching
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.github.ben-manes.caffeine:jcache")
    implementation("javax.cache:cache-api")

    // Data, Quartz & PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    runtimeOnly("org.postgresql:postgresql")

    // Flyway
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Http Client
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect")

    // Lombok, MapStruct & Config Processor
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    implementation("org.modelmapper:modelmapper:$modelmapperVersion")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.3")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
    testImplementation("com.github.dasniko:testcontainers-keycloak:4.1.1")

    // Wiremock
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")

    // Mockito Agent
    mockitoAgent("org.mockito:mockito-core:$mockitoVersion")
}

// Configuración de recursos de prueba (equivalente a <testResources> en el POM)
tasks.processTestResources {
    from(project.layout.projectDirectory.dir("keycloak")) {
        include("realm-export.json")
        into("keycloak")
    }
}

jacoco {
    toolVersion = "0.8.14"
}

// Configuración base para las pruebas (equivalente a Surefire)
tasks.test {
    useJUnitPlatform()
    include("**/*Test.class")
    exclude("**/*IT.class")

    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-javaagent:${mockitoAgent.asPath}")
    })

    finalizedBy(tasks.jacocoTestReport)
}

// Reporte JaCoCo para Unit Tests
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    executionData(tasks.test.get())
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Configuración para Integration Tests (equivalente a Failsafe)
val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Ejecuta las pruebas de integración."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform()
    include("**/*IT.class")

    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-javaagent:${mockitoAgent.asPath}")
    })

    shouldRunAfter(tasks.test)
}

// Reporte JaCoCo para Integration Tests
val jacocoIntegrationTestReport = tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    dependsOn(integrationTest)
    executionData(integrationTest.get())
    sourceSets(sourceSets.main.get())

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Verificación de cobertura de JaCoCo (equivalente a check-it-coverage)
val jacocoIntegrationCoverageVerification = tasks.register<JacocoCoverageVerification>("jacocoIntegrationCoverageVerification") {
    dependsOn(jacocoIntegrationTestReport)
    executionData(integrationTest.get())
    sourceSets(sourceSets.main.get())

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.05".toBigDecimal()
            }
        }
    }
}

// Enlazar las tareas al ciclo de build (equivalente a la fase verify)
tasks.check {
    dependsOn(integrationTest)
    dependsOn(jacocoIntegrationCoverageVerification)
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}