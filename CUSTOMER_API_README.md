# Customer Management API

## Overview
Backend APIs for managing customers in the `customer_users` table using the existing `User` entity.

## Files Created

### DTOs
1. **CustomerCreateRequest.java** - Request DTO for creating customers
2. **CustomerUpdateRequest.java** - Request DTO for updating customers  
3. **CustomerResponse.java** - Response DTO for customer data

### Controller
4. **CustomerController.java** - REST controller with all CRUD endpoints

## API Endpoints

### Base URL: `/customers`

### 1. Get All Customers
```
GET /customers
```
**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "enabled": true,
    "roleId": 8,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
]
```

### 2. Get Customer by ID
```
GET /customers/{id}
```
**Response:** `200 OK` or `404 Not Found`
```json
{
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "roleId": 8,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

### 3. Create Customer
```
POST /customers
Content-Type: application/json
```
**Request Body:**
```json
{
  "email": "newuser@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "password": "password123",
  "enabled": true,
  "roleId": 8
}
```
**Response:** `201 Created` or `400 Bad Request` or `409 Conflict`
```json
{
  "id": 2,
  "email": "newuser@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "enabled": true,
  "roleId": 8,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00"
}
```

### 4. Update Customer
```
PUT /customers/{id}
Content-Type: application/json
```
**Request Body:**
```json
{
  "email": "updated@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "password": "newpassword123",
  "enabled": false,
  "roleId": 8
}
```
**Notes:**
- Password is optional - leave empty to keep current password
- All other fields are required

**Response:** `200 OK` or `404 Not Found` or `400 Bad Request` or `409 Conflict`

### 5. Delete Customer
```
DELETE /customers/{id}
```
**Response:** `200 OK` or `404 Not Found`
```json
"Customer deleted successfully"
```

## Features

### Security
- Passwords are encrypted using BCrypt via `PasswordEncoder`
- Email uniqueness is enforced
- Validation for required fields

### Validation
- Email format validation
- Required field checks (email, firstName, lastName, password for create)
- Duplicate email detection
- Password encryption

### Error Handling
- `400 Bad Request` - Missing or invalid required fields
- `404 Not Found` - Customer ID not found
- `409 Conflict` - Email already exists
- `500 Internal Server Error` - Server errors with detailed logging

## Database
Uses the existing `User` entity which maps to the `customer_users` table with fields:
- `id` (Primary Key, Auto-increment)
- `email` (Unique, Not Null)
- `password` (Not Null, Encrypted)
- `first_name`
- `last_name`
- `enabled` (Boolean, Default: true)
- `role_id` (Foreign Key to roles table)
- `created_at` (Auto-generated)
- `updated_at` (Auto-updated)

## Testing

### Using cURL

**Get all customers:**
```bash
curl -X GET http://localhost:8080/customers
```

**Create customer:**
```bash
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "password": "password123",
    "enabled": true,
    "roleId": 8
  }'
```

**Update customer:**
```bash
curl -X PUT http://localhost:8080/customers/1 \
  -H "Content-Type: application/json" \
  -d '{
    "email": "updated@example.com",
    "firstName": "Updated",
    "lastName": "User",
    "enabled": true,
    "roleId": 8
  }'
```

**Delete customer:**
```bash
curl -X DELETE http://localhost:8080/customers/1
```

## Integration with Flutter App

The Flutter app is already configured to use these endpoints:
- Model: `lib/models/customer.dart`
- Service: `lib/services/crm_service.dart` (Customer methods)
- Screens: `lib/screens/customers/`

Make sure your Flutter app's `AppConfig` points to the correct API base URL.

## Notes

- The API uses the existing `User` entity and `UserRepository`
- Role management is handled separately via the `Role` entity
- Authentication/Authorization should be configured in `SecurityConfig.java`
- Consider adding pagination for large customer lists in the future

