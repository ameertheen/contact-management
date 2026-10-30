# Contact Management - API Design

Base URL: /api/contacts

## 1. Create Contact

POST /api/contacts

Request:
{
  "firstName": "John",
  "lastName": "David",
  "email": "john@example.com",
  "phone": "9876543210",
  "company": "ABC Technologies",
  "jobTitle": "Software Engineer",
  "address": "Chennai",
  "notes": "Business contact"
}

## 2. Get All Contacts

GET /api/contacts

## 3. Get Contact By ID

GET /api/contacts/{id}

## 4. Update Contact

PUT /api/contacts/{id}

Request: Same contact fields as create.

## 5. Delete Contact

DELETE /api/contacts/{id}

## 6. Search Contacts

GET /api/contacts?search=john

Search fields:
- First name
- Last name
- Email
- Phone

## 7. Expected HTTP Status Codes

- 200: Successful GET or UPDATE
- 201: Contact created
- 204: Contact deleted
- 400: Validation error
- 404: Contact not found
- 409: Duplicate email