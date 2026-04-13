# AGENTS.md - LMS Backend Developer Guide

## Project Overview

This is a **Learning Management System (LMS) backend** built with **Spring Boot 3.5.7** and **Java 17**. It's a multi-tenant educational platform with course management, quiz/assignment features, and user authentication.

### Core Architecture

- **Framework**: Spring Boot (REST API, OAuth2, JPA)
- **Database**: MySQL with Hibernate ORM
- **Authentication**: JWT + Google OAuth2
- **File Storage**: Cloudinary (cloud-based image/file storage)
- **Communication**: WebSocket for real-time features, REST for standard APIs
- **Build Tool**: Maven

---

## Essential Architecture Knowledge

### Layered Structure

The application follows a classic Spring layered architecture:

```
controller/ → service/ → repository/ → entity/
            ↓
        dto/ (data transfer)
            ↓
    exception/ (error handling)
```

**Key layers**:
- **Controllers** (`controller/`): REST endpoints mapped to `/api/v1/lms/*`
- **Services** (`service/impl/`): Business logic with interface contracts
- **Repositories** (`repository/`): JPA Spring Data repositories for DB access
- **Entities** (`entity/`): JPA mapped database models
- **DTOs** (`dto/request/`, `dto/response/`): Request/response payload objects

### Data Flow Example: Login

1. `AuthController.login()` receives `LoginRequest` DTO
2. Routes to `AuthService.login()` interface
3. `AuthServiceImpl` authenticates via Spring's `AuthenticationManager`
4. `SecurityUtil` generates JWT tokens (access + refresh)
5. Returns `LoginResponse` with user info and tokens
6. Refresh token stored in HTTP-only cookie for security

### Base Entity Pattern

All entities inherit from `BaseEntity` (soft delete + auditing):

```
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedBy @CreatedDate
    @LastModifiedBy @LastModifiedDate
    @Column(name = "is_deleted") // Soft delete: UPDATE instead of DELETE
    @Column(name = "is_active")  // Logical deactivation
}
```

Entities use `@SQLDelete` + `@SQLRestriction` annotations for automatic soft-delete handling.
**Important**: Every query automatically filters `is_deleted = false`.

### Security Configuration

- **JWT**: Custom `SecurityUtil` creates access/refresh tokens with role claims
- **OAuth2**: Google login via `GoogleOAuth2SuccessHandler` 
- **Role-based access**: `@PreAuthorize("hasRole('ROLE_XXX')")` on methods
- **CORS**: Configured in `CorsConfig`
- **Session**: Stateless (JWT-based) - `SessionCreationPolicy.STATELESS`

### Critical Configuration Files

- `application.properties`: Default config
- `application-dev.properties`: Development overrides (environment variables via dotenv)
- `application-prod.properties`: Production overrides
- Environment loading: `BackendApplication.main()` loads `.env` file in dev mode

---

## Key Components & Cross-Cutting Concerns

### Enums & Constants (`constant/`)

16 enums define domain types (don't add magic strings):

```
ContentItemType (LESSON, QUIZ, ASSIGNMENT)
CourseStatus, EnrollmentStatus, RoleType
QuestionType, DifficultyLevel
OtpType, AttemptStatus
```

Use these in queries and business logic checks, e.g.:
```java
if (chapter.getContentType() == ContentItemType.QUIZ) { ... }
```

### File/Image Management

- **Cloudinary integration** (`CloudinaryConfig`, `CloudinaryService`):
  - Images uploaded to cloud storage
  - Stores `cloudinaryImageId` for deletion/updates
  - User avatars, course thumbnails, resource uploads
- **Large file support**: `spring.servlet.multipart.max-file-size=500MB`

### Email & OTP System

- **OTP Service** (`OtpService`, `OtpRepository`):
  - Email-based verification for registration and password reset
  - Types: `OtpType.REGISTER`, `OtpType.RESET_PASSWORD`
  - Gmail SMTP configured in `application.properties`
- **Dependency**: `spring-boot-starter-mail`

### Query Patterns

**Specifications** (`specification/`):
- `CourseSpecification`, `PermissionSpecification`, `UserSpecification`
- Dynamic filtering without hardcoding WHERE clauses
- Example: Filter courses by status, keyword search

**Custom Repository Methods**:
- `UserRepository.findByUserNameAndRefreshToken()` for token validation
- `OtpRepository` lookup by email and OTP type
- Standard `findById()`, `save()`, `delete()` inherited

### Exception Handling

Global exception handler in `GlobalExceptionHandler`:
- Custom exceptions: `BusinessException`, `ResourceNotFoundException`, `UnauthorizedException`
- Automatically catches `MethodArgumentNotValidException` for validation errors
- Returns standardized `ApiResponse<T>` format
- HTTP status codes mapped appropriately (400, 404, 500)

**Pattern for new endpoints**:
```java
throw new BusinessException("Validation message");
throw new ResourceNotFoundException("Entity not found");
```

### Response Structure

All endpoints return `ApiResponse<T>`:
```java
ApiResponse<UserDTO> response = new ApiResponse<>(200, "Success", data);
```

---

## Domain Model Highlights

### Key Entities

**User-centric**:
- `User`: Core user with roles, OAuth2 google info, profile fields
- `Role`: Role-based access (STUDENT, INSTRUCTOR, ADMIN)
- `Permission`: Fine-grained access control

**Course Structure** (hierarchical):
- `Course` → `Chapter` → `ChapterItem` → Content (Lesson, Quiz, Assignment)
- `Enrollment`: User-Course mapping with status
- `StudentChapterItemProgress`: Tracks completion state

**Assessment**:
- `Quiz` + `QuizQuestion` + `BankQuestion` (reusable question pool)
- `QuizAttempt` + `QuizAttemptAnswer` (user responses)
- `QuizAnswer`: Expected answer structure
- `Assignment`: Similar attempt/answer tracking

**Metadata**:
- `Category`, `Subject`, `QuestionTag`: Taxonomies
- `Resource`: Attachments (documents, videos)
- `Meeting`: Virtual class sessions
- `Comment`: Discussion/feedback on content

---

## Developer Workflows

### Building & Running

**Maven build**:
```powershell
mvn clean install              # Full build with tests
mvn clean install -DskipTests  # Skip tests (faster)
```

**Running locally**:
```powershell
# Development (loads .env):
java -jar target/backend-0.0.1-SNAPSHOT.jar

# Or via Spring Boot Maven plugin:
mvn spring-boot:run

# With specific profile:
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

**Environment setup**:
- Create `.env` file in project root for dev mode
- Required vars: `JWT_BASE64_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `CLOUDINARY_*`, `GMAIL*`
- Production: Set OS environment variables instead

### Testing

- Unit tests in `src/test/java/`
- Test class: `BackendApplicationTests.java`
- Run with: `mvn test` or `mvn clean install`
- Security testing: `spring-security-test` dependency included

### Database Management

- **Hibernate DDL**: `spring.jpa.hibernate.ddl-auto=update` (auto-creates/alters tables)
- **Soft deletes**: Queries automatically filter deleted rows via `@SQLRestriction`
- **MySQL 8.x+** required
- Connection config via `spring.datasource.*` properties

### API Documentation

- **Swagger/OpenAPI** enabled at: `http://localhost:8080/swagger-ui.html`
- Config: `OpenAPIConfig.java`
- Dependency: `springdoc-openapi-starter-webmvc-ui`
- Document endpoints with `@Operation`, `@Schema` annotations

---

## Important Patterns & Conventions

### Service Interface + Implementation Split

**Why**: Loose coupling, easier testing, clear contracts

```java
// Interface: AuthService
public interface AuthService { ... }

// Implementation: AuthServiceImpl
@Service
public class AuthServiceImpl implements AuthService { ... }
```

Constructor injection for all dependencies (no `@Autowired` fields).

### DTO Naming Convention

- **Request**: `{Entity}Request` or `{Action}Request` (e.g., `LoginRequest`, `RegisterRequest`)
- **Response**: `{Entity}Response` or simple `{Entity}DTO` (e.g., `UserInfoResponse`)
- **Nested**: Use inner classes for structured data

### Entity Auditing

Fields automatically populated:
- `createdBy`: Username from `SecurityContext`
- `createdDate`: Insertion timestamp
- `lastModifiedBy`: Last user to update
- `lastModifiedDate`: Last update timestamp
- Configured via `AuditorConfig` + `UserDetailCustom`

### Soft Delete Pattern

Never hardcode `DELETE FROM users`. Hibernate handles it via `@SQLDelete`:
```java
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE user_id = ?")
@SQLRestriction(value = "is_deleted = false")
```

### Role-Based Access Control

Use annotations on service methods:
```java
@PreAuthorize("hasRole('ROLE_ADMIN')")
public void deleteUser(Integer id) { ... }

@PreAuthorize("hasAnyRole('ROLE_STUDENT', 'ROLE_INSTRUCTOR')")
public void enrollCourse(Integer courseId) { ... }
```

Verify role is a `RoleType` enum value defined in `constant/`.

---

## Critical Integration Points

### JWT Token Management

- **Access token**: Short-lived (8640 seconds = 2.4 hours), for API calls
- **Refresh token**: Long-lived (864000 seconds = 10 days), stored in DB + HTTP-only cookie
- Tokens contain: username, role claim (required by `JwtAuthenticationConverter`)
- Refresh flow: `AuthController.refreshToken()` endpoint exchanges old refresh for new access

### Google OAuth2 Flow

1. Frontend redirects to `/oauth2/authorization/google`
2. Spring Security handles provider interaction
3. `GoogleOAuth2SuccessHandler` intercepts successful auth
4. Creates/updates User, generates JWT tokens
5. Redirects to frontend with tokens in URL/cookie

### Cloudinary Upload Flow

1. `UploadController` receives multipart file
2. `CloudinaryService.uploadFile()` sends to Cloudinary API
3. Response includes public URL + unique `cloudinaryImageId`
4. Store ID in entity for later deletion/updates
5. For user avatars: Update `User.cloudinaryImageId` before deletion to cleanup old files

### Email/OTP Flow

1. `OtpService.sendOtp()` generates random 6-digit OTP
2. Sends via `JavaMailSender` (Gmail SMTP)
3. Stores `Otp` entity with email, type, expiry
4. `AuthService` verifies OTP before marking user verified
5. Password reset uses same OTP mechanism

---

## Common Gotchas & Best Practices

- **Soft deletes**: Always filter `is_deleted = false` in custom queries—use specifications or add `@SQLRestriction`
- **JWT claims**: Ensure role claim is included; `JwtAuthenticationConverter` expects it
- **Cloudinary IDs**: Store the ID, not just the URL, for proper cleanup on updates/deletes
- **OTP expiry**: Check timestamp in `OtpService` before verification
- **Entity relationships**: Use `mappedBy` for bidirectional JPA relations to avoid duplicate joins
- **Transaction boundaries**: Service methods are `@Transactional` by default; be aware of lazy-loading
- **CORS configuration**: Frontend URL must be whitelisted in `CorsConfig`
- **Password hashing**: Always use injected `PasswordEncoder` (BCrypt), never store plaintext

---

## Dockerfile & Deployment

- Dockerfile included for containerization
- Multi-stage build recommended for size optimization
- Ensure environment variables injected at runtime
- Health check endpoint recommended (not yet implemented)

---

## Entry Points for AI Agents

Start with these files to understand any feature:

1. **Authentication**: `AuthController` → `AuthServiceImpl` → `SecurityUtil` (JWT generation)
2. **Database schema**: `entity/User.java`, `entity/Course.java`, `entity/Enrollment.java`
3. **New API endpoint**: Copy pattern from `CourseController` → `CourseService` → `CourseRepository`
4. **File uploads**: `UploadController` → `CloudinaryService` → `CloudinaryConfig`
5. **Email/OTP**: `OtpService` → Spring Mail configuration in `application.properties`
6. **Role-based logic**: Check `RoleType` enum + `@PreAuthorize` annotations
7. **Error responses**: `GlobalExceptionHandler` + `ApiResponse` structure

---

## Useful Maven Commands

```powershell
mvn clean compile              # Compile only
mvn clean test                 # Run tests
mvn clean package -DskipTests  # Build JAR without tests
mvn dependency:tree            # View dependency hierarchy
mvn spring-boot:run            # Run dev server
```

---

## Summary

This LMS backend is a feature-rich multi-user educational platform with JWT security, cloud storage integration, and complex course/assessment hierarchies. The codebase follows Spring conventions strictly: layered architecture, interface-driven services, soft-delete patterns, and centralized error handling. Familiarize yourself with the entity relationships, security configurations, and soft-delete mechanics before making changes.

