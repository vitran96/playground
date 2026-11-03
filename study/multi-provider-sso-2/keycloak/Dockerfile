# Keycloak + demo SPI (email-OTP password reset, external-only registration).
# Compiled with plain javac against the exact Keycloak jars, no Maven needed.
FROM quay.io/keycloak/keycloak:26.8.1 AS kc

FROM docker.io/library/eclipse-temurin:21-jdk AS build
COPY --from=kc /opt/keycloak/lib/lib /kc
COPY spi/src /src
RUN mkdir /out && javac --release 21 -nowarn -cp "/kc/main/*:/kc/boot/*" -d /out $(find /src -name '*.java') \
 && cp -r /src/META-INF /out/ && jar cf /demo-spi.jar -C /out .

FROM quay.io/keycloak/keycloak:26.8.1
COPY --from=build /demo-spi.jar /opt/keycloak/providers/demo-spi.jar
