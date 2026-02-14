# Portal API

REST API for internal portal features including CRM, project management, vendor management, and administrative functions.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Access to the database server

## Setup

### 1. Environment Configuration

Copy the example environment file and configure with your values:

```bash
cp .env.example .env
```

Edit `.env` and set your actual values:

```properties
# Database Configuration
DB_URL=jdbc:postgresql://your-host:5432/your_database
DB_USERNAME=your_username
DB_PASSWORD=your_secure_password

# JWT Configuration - Generate with: openssl rand -hex 32
JWT_SECRET=your_generated_secret_key
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Email Configuration
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your_email@example.com
MAIL_PASSWORD=your_email_password

# File Storage Path (use \\ for Windows paths)
STORAGE_BASE_PATH=/path/to/storage
```

### 2. Database Setup

Create the database:

```sql
CREATE DATABASE your_database_name;
```

### 3. Flyway Configuration

If using Flyway for migrations, configure Maven properties or environment variables:

```bash
export FLYWAY_URL=jdbc:postgresql://your-host:5432/your_database
export FLYWAY_USER=your_username
export FLYWAY_PASSWORD=your_password
```

### 4. Build and Run

```bash
# Build the project
mvn clean install

# Run migrations (if using Flyway)
mvn flyway:migrate

# Run the application
mvn spring-boot:run
```

The API will start on `http://localhost:8081` (or the port specified in `SERVER_PORT`).

## Configuration

### Production Settings

For production deployment, set these environment variables:

```bash
JPA_DDL_AUTO=validate            # Never use 'update' or 'create' in production
JPA_SHOW_SQL=false               # Disable SQL logging
LOGGING_LEVEL=INFO               # Use INFO level logging
```

### Shared Storage

The `STORAGE_BASE_PATH` should point to a shared directory accessible by both:
- `wd_portal_api` (for uploads)
- `wd_customer_api` (for downloads)

This allows documents uploaded via the portal to be visible in the customer app.

### Security Best Practices

1. **Never commit `.env` files** - They contain sensitive credentials
2. **Rotate secrets regularly** - See `SECURITY.md` for instructions
3. **Use strong passwords** - Minimum 12 characters with mixed case, numbers, and symbols
4. **Generate JWT secrets properly** - Use `openssl rand -hex 32` or similar
5. **Use environment-specific configs** - Different secrets for dev/staging/production
6. **Secure email credentials** - Use app-specific passwords or OAuth where possible

## API Documentation

Once running, access the API at:
- Base URL: `http://localhost:8081`
- Health Check: `http://localhost:8081/actuator/health`

### Key Features

- **CRM**: Lead management, scoring, and tracking
- **Projects**: Project lifecycle management
- **Tasks**: Task assignment and tracking
- **Documents**: Document upload and management
- **Vendors**: Vendor and partnership management
- **Accounts**: Accounts payable and financial tracking
- **Site Reports**: Construction site reporting and photo management

## CORS Configuration

CORS is configured globally in `SecurityConfig`. Per-controller `@CrossOrigin` annotations have been removed for centralized management.

## Troubleshooting

### Database Connection Issues

If you see connection errors:
1. Verify database is running
2. Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in `.env`
3. Ensure database exists
4. Check network connectivity to database server

### Email Sending Issues

If emails aren't sending:
1. Verify SMTP settings in `.env`
2. Check if your email provider requires app-specific passwords
3. Ensure firewall allows outbound SMTP connections
4. Check application logs for detailed error messages

### File Storage Issues

If file uploads/downloads fail:
1. Verify `STORAGE_BASE_PATH` exists and is writable
2. Check disk space availability
3. Ensure proper permissions on storage directory
4. Verify path format (use `\\` for Windows, `/` for Linux)

## Development

### Code Style

- Use explicit imports (no wildcards)
- Use SLF4J logger (no System.out.println or printStackTrace)
- Always log exceptions with context
- Remove `@SuppressWarnings("null")` - fix null safety issues properly
- Follow Spring Boot best practices

### Database Migrations

Consider using Flyway or Liquibase for production database changes instead of Hibernate's DDL auto-update.

## Security

See `SECURITY.md` for security guidelines and incident response procedures.
