# Security Guidelines

## Rotating Secrets

If secrets have been exposed (e.g., committed to git), follow these steps immediately:

### 1. Generate New Secrets

```bash
# Generate new JWT secret
openssl rand -hex 32

# For other secrets, use your provider's secret rotation tools
```

### 2. Update Environment Variables

Update `.env` with new values:

```properties
JWT_SECRET=<new_generated_secret>
DB_PASSWORD=<new_database_password>
MAIL_PASSWORD=<new_email_password>
```

### 3. Update Database Passwords

Connect to your database and change the password:

```sql
ALTER USER your_username WITH PASSWORD 'new_secure_password';
```

### 4. Update Email Passwords

If using app-specific passwords:
1. Log into your email provider
2. Revoke old app-specific password
3. Generate new app-specific password
4. Update `MAIL_PASSWORD` in `.env`

### 5. Restart Services

After updating secrets:

```bash
# Stop the application
# Update .env file
# Restart the application
mvn spring-boot:run
```

### 6. Invalidate Old Sessions

When rotating JWT secrets, all existing user sessions will be invalidated automatically. Users will need to log in again.

## Removing Secrets from Git History

If secrets were committed to git:

### Option 1: Using git filter-branch (for local cleanup)

```bash
# Remove specific files from all commits
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env .env.staging" \
  --prune-empty --tag-name-filter cat -- --all

# Force push (ONLY if you haven't shared the repository)
git push origin --force --all
```

### Option 2: Using BFG Repo-Cleaner (recommended)

```bash
# Install BFG
# https://rtyley.github.io/bfg-repo-cleaner/

# Remove all .env files from history
bfg --delete-files .env

# Clean up
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

### Option 3: Fresh Repository (safest for already-shared repos)

1. Create a new repository
2. Copy files (excluding .env files)
3. Commit to new repository
4. Update remote URL
5. Archive old repository

## Security Checklist

### Before Deployment

- [ ] All `.env` files are in `.gitignore`
- [ ] `.env.example` exists with no real secrets
- [ ] All secrets are environment-specific (dev ≠ staging ≠ production)
- [ ] JWT secrets are at least 32 characters (256 bits)
- [ ] Database passwords are strong (12+ characters, mixed case, numbers, symbols)
- [ ] Email passwords are app-specific (not main account password)
- [ ] `JPA_DDL_AUTO` is set to `validate` (never `update` or `create`)
- [ ] `JPA_SHOW_SQL` is set to `false`
- [ ] Logging level is `INFO` or `WARN` (not `DEBUG`)
- [ ] CORS origins are restricted (not `*`)
- [ ] File upload paths are outside web root
- [ ] Flyway credentials use environment variables (not hardcoded in pom.xml)

### Regular Maintenance

- [ ] Rotate JWT secrets every 90 days
- [ ] Rotate database passwords every 90 days
- [ ] Rotate email passwords every 90 days
- [ ] Review and update dependencies monthly
- [ ] Monitor for security vulnerabilities
- [ ] Review access logs regularly
- [ ] Audit file upload directory permissions

## Known Security Considerations

### JSON Query String Manipulation

The `FeedbackResponseRepository.getAverageRatingByFormId()` method uses string manipulation on JSON data, which could be fragile. Consider:

- Using PostgreSQL JSON functions (`jsonb_extract_path_text`)
- Parsing JSON in application code with Jackson/Gson
- Storing ratings in a separate column

### CORS Configuration

CORS is configured globally in `SecurityConfig`. Review allowed origins regularly and restrict to specific domains in production.

## Incident Response

If you discover a security incident:

1. **Immediately** rotate all affected credentials
2. Review logs for unauthorized access
3. Check file uploads for malicious content
4. Notify relevant stakeholders
5. Document the incident
6. Implement measures to prevent recurrence

## Reporting Security Issues

If you discover a security vulnerability, please report it to:
- Email: [Your Security Contact Email]
- Do NOT open a public GitHub issue

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Best Practices](https://spring.io/guides/topicals/spring-security-architecture)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)
