# Contact Management - Architecture

## 1. Architecture Style

Three-layer full-stack application with a REST API.

## 2. Frontend

Angular 16 application.

Responsibilities:
- Display contact information.
- Manage contact forms.
- Perform client-side validation.
- Communicate with backend REST APIs.
- Handle loading and error states.

## 3. Backend

Spring Boot application.

Layers:

- Controller: Handles HTTP requests and responses.
- Service: Contains business logic.
- Repository: Handles database operations.
- Entity: Represents database records.
- DTO: Defines API request and response structures.
- Exception: Centralized exception handling.

## 4. Database

SQLite relational database.

Initial table: contacts.

The database file must be stored outside temporary container storage when deployed.

## 5. Communication

Angular communicates with Spring Boot using HTTP and JSON.

## 6. High-Level Flow

User -> Angular Component -> Angular Service -> REST Controller -> Service -> Repository -> SQLite

## 7. Design Principles

- Separation of concerns.
- Single responsibility.
- DTO-based API communication.
- Input validation.
- Consistent error responses.
- Testable business logic.