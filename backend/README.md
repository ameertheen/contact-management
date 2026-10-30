# Contact Management Backend

Spring Boot REST API for the Contact Management System. This module currently provides the project foundation: configuration, the `contacts` table mapping, and a health check. Contact CRUD endpoints are not implemented yet.

## Prerequisites

- Java 17
- Maven 3.9 or newer

Check both from a terminal:

```bash
java -version
mvn -version
```

## Setup

From the `backend` directory:

```bash
mvn test
mvn package
```

`mvn package` writes an executable JAR to `backend/target/contact-management-0.0.1-SNAPSHOT.jar`.

## Run

From the `backend` directory:

```bash
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/contact-management-0.0.1-SNAPSHOT.jar
```

The application listens on port 8080. On startup it creates `backend/data/` if needed and stores the SQLite database at `backend/data/contacts.db`. Run these commands from `backend/` so that relative path stays inside this module.

Health check:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{"status":"UP"}
```

## Package structure

```text
com.contactmanagement
├── ContactManagementApplication
├── controller    HTTP endpoints
├── service       business logic (added with the contact API)
├── repository    database access
├── entity        JPA entities
├── dto           API request and response objects (added with the contact API)
├── exception     centralized error handling (added with the contact API)
└── config        Spring configuration (added when custom beans are required)
```

## Configuration

SQLite settings live in `src/main/resources/application.properties`.

- JDBC URL: `jdbc:sqlite:data/contacts.db`
- Dialect: `org.hibernate.community.dialect.SQLiteDialect`
- Schema mode: `update`, so Hibernate creates or updates tables from the entity mappings
- Connection pool size: 1, which matches SQLite's single-writer behavior

Tests use a separate database file, `target/contacts-test.db`, so they do not write to the development database.
