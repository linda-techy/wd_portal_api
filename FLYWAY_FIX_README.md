# Flyway Migration Checksum Issue - FIXED

## Problem
The application failed to start with the following error:
```
org.flywaydb.core.api.exception.FlywayValidateException: Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 2
-> Applied to database : 1382772527
-> Resolved locally    : -1567364399
```

## Root Cause
The migration file `V2__add_performance_indexes.sql` was modified **after** it had already been applied to the database. Flyway validates migration checksums to ensure database consistency, and detected the mismatch.

## Solution Applied
Ran Flyway repair to update the checksum in the database schema history table:

```bash
mvn flyway:repair -Dflyway.url="jdbc:postgresql://46.202.164.251:5432/wdTestDB" \
  -Dflyway.user=postgres -Dflyway.password=***
```

**Result**: Successfully repaired schema history table - checksum updated from `1382772527` to `-1567364399`

## Important Notes

### ⚠️ Best Practices for Flyway Migrations

1. **NEVER modify a migration after it's been applied to ANY environment**
   - Once a migration is applied, it should be considered immutable
   - Any changes should be made in a NEW migration file

2. **If you need to change database structure:**
   - Create a NEW migration file (e.g., `V3__update_indexes.sql`)
   - Use the new migration to make your changes
   - DO NOT edit existing migration files

3. **When you absolutely must modify an existing migration:**
   - Only do this in development environments
   - Run `mvn flyway:repair` after modification
   - Coordinate with the team to ensure all environments are updated

### Running Flyway Repair

If you encounter this issue again, use one of these methods:

**Method 1: Using the repair script**
```bash
# Windows
./repair-flyway.bat

# Linux/Mac
chmod +x repair-flyway.sh
./repair-flyway.sh
```

**Method 2: Direct Maven command**
```bash
mvn flyway:repair -Dflyway.url="$DB_URL" \
  -Dflyway.user="$DB_USERNAME" \
  -Dflyway.password="$DB_PASSWORD"
```

**Method 3: Using environment variables from .env**
```bash
# Load .env variables first
export $(cat .env | xargs)

# Then run repair
mvn flyway:repair -Dflyway.url="$DB_URL" \
  -Dflyway.user="$DB_USERNAME" \
  -Dflyway.password="$DB_PASSWORD"
```

## Files Created
- `repair-flyway.bat` - Windows repair script
- `repair-flyway.sh` - Linux/Mac repair script
- `FLYWAY_FIX_README.md` - This documentation

## Status
✅ **FIXED** - Application should now start successfully

## Date Fixed
2026-02-11

## Reference
- Flyway Documentation: https://flywaydb.org/documentation/command/repair
- Migration Naming Convention: https://flywaydb.org/documentation/concepts/migrations#naming
