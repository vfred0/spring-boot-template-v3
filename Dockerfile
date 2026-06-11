FROM eclipse-temurin:25-jdk-alpine@sha256:30d9f87d702c2c1c601ed0d31e0c88ea1ea474ee7676cda7b7a59e759181c4dd AS builder
WORKDIR /build

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY build.gradle.kts settings.gradle.kts ./

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies \
    --no-daemon \
    --parallel \
    --build-cache \
    --configuration-cache \
    --max-workers=$(nproc) \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:+UseG1GC"

COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar \
    --no-daemon \
    --parallel \
    --build-cache \
    --configuration-cache \
    --max-workers=$(nproc) \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:+UseG1GC"

FROM eclipse-temurin:25-jre-alpine@sha256:c707c0d18cb9e8556380719f80d96a7529d0746fbb42143893949b98ed2f8943
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
RUN mkdir -p /app/data/downloads && chown -R app:app /app/data

COPY --from=builder --chown=app:app /build/build/libs/*.jar app.jar

USER app
EXPOSE 8081

ENTRYPOINT ["java", \
  "--enable-preview", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]