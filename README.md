# Nefentus API

## Requirements

- Java 17
- Docker


## Test and build

Build with gradle using: `./gradlew clean build` 

This creates a jar file.

## Run

1. Create file .env locally with the same variables as in `.env_example`
2. Create folder `db` in the root of the project
3. Set `spring.profiles.active=${SPRING_PROFILE:dev}` in `application.properties` for dev mode
4. Start Nefentus API in docker compose: `docker compose up --build`
	The first time it runs, the API may fail because the database (localted in `./db`) is still being created. Wait until it is finished and then start again.
