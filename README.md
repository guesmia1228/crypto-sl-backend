# Nefentus API

## Requirements

- Java 17
- Docker


## Test and build

Build with gradle using: `./gradlew clean build` 

This creates a jar file.

## Run

1. Create file .env locally with the same variables as in `.env_example`
2. Start Nefentus API in docker compose: `docker compose up --build`
	The first time it runs, the API may fail because the database (localted in ./db) is still being created.
