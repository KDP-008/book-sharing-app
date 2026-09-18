# Release Notes Draft - v1.1.0

## Key Updates & Deprecations
- **Return Workflow**: Removed `POST /books/{bookId}/return` endpoint and replaced it with automated returns.
- **Verification**: User registration now requires phone number verification.
- **BREAKING CHANGE**: The `POST /auth/login` endpoint has been removed. All authentication has migrated to `/auth/token`.
