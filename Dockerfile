# base image to build a JRE
FROM amazoncorretto:17.0.3-alpine as corretto-jdk

# required for strip-debug to work
RUN apk add --no-cache binutils

# Build small JRE image
RUN $JAVA_HOME/bin/jlink \
         --verbose \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /customjre

FROM alpine:latest
ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

LABEL org.label-schema.name="nefentus"

WORKDIR /app

# copy JRE from the base image
COPY --from=corretto-jdk /customjre $JAVA_HOME

# RUN useradd --shell /bin/bash app

COPY build/libs/api-0.0.1-SNAPSHOT.jar /app/app.jar

CMD ["java","-jar", "app.jar"]