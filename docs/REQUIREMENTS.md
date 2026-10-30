# Contact Management System

## 1. Project Overview

A full-stack web application to manage personal and business contacts.

## 2. Technology Stack

- Backend: Java 17, Spring Boot 3.x
- Database: SQLite
- Frontend: Angular 16
- Styling: Bootstrap 5
- Version Control: Git and GitHub
- CI/CD: GitHub Actions

## 3. Functional Requirements

### Contact Management
- Create a new contact.
- Display all contacts.
- View individual contact details.
- Update an existing contact.
- Delete a contact with confirmation.
- Search contacts by first name, last name, email or phone.

### Contact Information

- First name (required)
- Last name (required)
- Email (required and unique)
- Phone (required)
- Company (optional)
- Job title (optional)
- Address (optional)
- Notes (optional)

## 4. Validation Requirements

- First name and last name cannot be empty.
- Email must have a valid format.
- Email must be unique.
- Phone number is required.
- Display meaningful validation messages.

## 5. Non-Functional Requirements

- REST API architecture.
- Layered backend architecture.
- Responsive Angular interface.
- Centralized exception handling.
- Automated backend and frontend tests.
- Environment-based configuration.
- Maintainable and readable code.

## 6. Out of Scope for Version 1

- User authentication.
- Role-based access.
- Contact import/export.
- Profile pictures.
- Email and SMS integration.