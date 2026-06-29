# Prop Ease

Enterprise-style rental property management REST API built with Java 21, Spring Boot 3, PostgreSQL, Flyway, JWT, JPA Specifications, MapStruct, Bean Validation, SMTP email, OpenAPI, Docker, JUnit 5, Mockito, MockMvc, and JaCoCo.

## Architecture

The code uses a layered package structure: controllers accept validated DTOs, transactional services enforce business ownership rules, MapStruct converts domain models, repositories isolate persistence, and a stateless security filter authenticates JWTs. Entities never leave the service layer. Users may simultaneously hold OWNER, TENANT, and GUIDE roles.

```text
HTTP / Swagger -> SecurityFilterChain + JWT filter -> Controllers
                                                   -> Services -> Repositories -> PostgreSQL
                                                               -> Image storage
                                                               -> SMTP / SES
```

Local development stores uploaded images in `uploads/`. `ImageStorageService` is the extension point for an S3 implementation in production.

## ER diagram

```mermaid
erDiagram
  USERS }o--o{ ROLES : user_roles
  USERS ||--o{ PROPERTIES : owns
  PROPERTIES ||--o{ PROPERTY_IMAGES : has
  USERS ||--o{ CONTACT_REQUESTS : submits
  PROPERTIES ||--o{ CONTACT_REQUESTS : receives
  USERS { uuid id PK string email string full_name }
  ROLES { bigint id PK string name }
  PROPERTIES { uuid id PK uuid owner_id FK decimal rent_amount string availability_status bigint views }
  PROPERTY_IMAGES { bigint id PK uuid property_id FK string image_url }
  CONTACT_REQUESTS { uuid id PK uuid tenant_id FK uuid property_id FK string status }
```

## Class diagrams

To edit a diagram visually in draw.io, copy the contents of its Mermaid block and use **Arrange → Insert → Advanced → Mermaid**. Keep the Mermaid source in this README as the version-controlled source of truth.

### Application-wide class diagram

This diagram shows the complete request path and the principal dependency direction. Solid arrows represent runtime dependencies; repository interfaces are implemented by Spring Data at runtime.

```mermaid
classDiagram
  direction LR

  class AuthController {
    +register(RegisterRequest) AuthResponse
    +login(LoginRequest) AuthResponse
    +refresh(RefreshRequest) AuthResponse
  }
  class PropertyController {
    +create(PropertyForm) PropertyResponse
    +update(UUID, PropertyForm) PropertyResponse
    +delete(UUID)
    +mine(Pageable) Page
    +details(UUID) PropertyResponse
    +search(filters, Pageable) Page
    +availability(UUID, AvailabilityRequest) PropertyResponse
    +analytics(UUID) AnalyticsResponse
  }
  class ContactRequestController {
    +create(CreateRequest) Response
    +list(Pageable) Page
    +approve(UUID) Response
    +reject(UUID) Response
  }

  class AuthService {
    +register(RegisterRequest) AuthResponse
    +login(LoginRequest) AuthResponse
    +refresh(RefreshRequest) AuthResponse
  }
  class PropertyService {
    +create(PropertyRequest, List) PropertyResponse
    +update(UUID, PropertyRequest, List) PropertyResponse
    +delete(UUID)
    +details(UUID) PropertyResponse
    +mine(Pageable) Page
    +search(filters, Pageable) Page
    +availability(UUID, AvailabilityRequest) PropertyResponse
    +analytics(UUID) AnalyticsResponse
  }
  class ContactRequestService {
    +create(CreateRequest) Response
    +ownerRequests(Pageable) Page
    +approve(UUID) Response
    +reject(UUID) Response
  }
  class CurrentUserService {
    +get() User
  }
  class EmailService {
    <<interface>>
    +sendApprovedContact(ContactRequest)
  }
  class SmtpEmailService
  class ImageStorageService {
    <<interface>>
    +store(MultipartFile) String
  }
  class LocalImageStorageService

  class UserRepository {
    <<interface>>
    +findByEmailIgnoreCase(String) Optional
  }
  class RoleRepository {
    <<interface>>
    +findByName(RoleName) Optional
  }
  class PropertyRepository {
    <<interface>>
    +findByOwnerId(UUID, Pageable) Page
    +incrementViews(UUID) int
  }
  class ContactRequestRepository {
    <<interface>>
    +findByPropertyOwnerId(UUID, Pageable) Page
    +countByPropertyId(UUID) long
  }

  class PropertyMapper {
    <<interface>>
    +toResponse(Property) PropertyResponse
    +toEntity(PropertyRequest) Property
    +update(PropertyRequest, Property)
  }
  class ContactRequestMapper {
    <<interface>>
    +toResponse(ContactRequest) Response
  }

  class JwtAuthenticationFilter {
    +doFilterInternal(request, response, chain)
  }
  class JwtTokenProvider {
    +accessToken(Authentication) String
    +refreshToken(Authentication) String
    +subject(String, String) String
    +validAccess(String) boolean
  }
  class CustomUserDetailsService {
    +loadUserByUsername(String) UserDetails
  }
  class SecurityConfig
  class GlobalExceptionHandler

  AuthController --> AuthService
  PropertyController --> PropertyService
  ContactRequestController --> ContactRequestService

  AuthService --> UserRepository
  AuthService --> RoleRepository
  AuthService --> JwtTokenProvider
  PropertyService --> PropertyRepository
  PropertyService --> ContactRequestRepository
  PropertyService --> PropertyMapper
  PropertyService --> ImageStorageService
  PropertyService --> CurrentUserService
  ContactRequestService --> ContactRequestRepository
  ContactRequestService --> PropertyRepository
  ContactRequestService --> ContactRequestMapper
  ContactRequestService --> CurrentUserService
  ContactRequestService --> EmailService
  CurrentUserService --> UserRepository

  SmtpEmailService ..|> EmailService
  LocalImageStorageService ..|> ImageStorageService
  JwtAuthenticationFilter --> JwtTokenProvider
  JwtAuthenticationFilter --> CustomUserDetailsService
  CustomUserDetailsService --> UserRepository
  SecurityConfig --> JwtAuthenticationFilter
  GlobalExceptionHandler ..> AuthController
  GlobalExceptionHandler ..> PropertyController
  GlobalExceptionHandler ..> ContactRequestController
```

### Domain class diagram

```mermaid
classDiagram
  direction TB

  class AuditableEntity {
    -Instant createdAt
    -Instant updatedAt
  }
  class User {
    -UUID id
    -String fullName
    -String email
    -String password
    -boolean enabled
    -Set~Role~ roles
  }
  class Role {
    -Long id
    -RoleName name
  }
  class Property {
    -UUID id
    -User owner
    -String title
    -String description
    -String address
    -String city
    -String area
    -BigDecimal rentAmount
    -Integer bedrooms
    -Integer bathrooms
    -AvailabilityStatus availabilityStatus
    -String contactEmail
    -String contactPhone
    -long views
    -List~PropertyImage~ images
  }
  class PropertyImage {
    -Long id
    -Property property
    -String imageUrl
  }
  class ContactRequest {
    -UUID id
    -User tenant
    -Property property
    -ContactRequestStatus status
    -Instant createdAt
  }
  class RoleName {
    <<enumeration>>
    ROLE_OWNER
    ROLE_TENANT
    ROLE_GUIDE
  }
  class AvailabilityStatus {
    <<enumeration>>
    AVAILABLE
    RENTED
    SOLD
  }
  class ContactRequestStatus {
    <<enumeration>>
    PENDING
    APPROVED
    REJECTED
  }

  AuditableEntity <|-- User
  AuditableEntity <|-- Property
  User "0..*" -- "0..*" Role : has
  User "1" --> "0..*" Property : owns
  Property "1" *-- "0..*" PropertyImage : images
  User "1" --> "0..*" ContactRequest : submits
  Property "1" --> "0..*" ContactRequest : receives
  Role --> RoleName
  Property --> AvailabilityStatus
  ContactRequest --> ContactRequestStatus
```

## Sequence diagrams

### Registration, login, and token refresh

```mermaid
sequenceDiagram
  autonumber
  actor Client
  participant AuthController
  participant AuthService
  participant AuthenticationManager
  participant UserRepository
  participant RoleRepository
  participant PasswordEncoder
  participant JwtTokenProvider

  alt Register
    Client->>AuthController: POST /api/v1/auth/register
    AuthController->>AuthService: register(validated request)
    AuthService->>UserRepository: existsByEmailIgnoreCase(email)
    UserRepository-->>AuthService: false
    loop Every requested role
      AuthService->>RoleRepository: findByName(ROLE_x)
      RoleRepository-->>AuthService: Role
    end
    AuthService->>PasswordEncoder: encode(password)
    PasswordEncoder-->>AuthService: BCrypt hash
    AuthService->>UserRepository: save(user with roles)
    AuthService->>AuthenticationManager: authenticate(email, password)
  else Login
    Client->>AuthController: POST /api/v1/auth/login
    AuthController->>AuthService: login(credentials)
    AuthService->>AuthenticationManager: authenticate(email, password)
  else Refresh
    Client->>AuthController: POST /api/v1/auth/refresh
    AuthController->>AuthService: refresh(refreshToken)
    AuthService->>JwtTokenProvider: subject(token, REFRESH)
    JwtTokenProvider-->>AuthService: email
    AuthService->>UserRepository: findByEmailIgnoreCase(email)
  end
  AuthService->>JwtTokenProvider: accessToken(authentication)
  AuthService->>JwtTokenProvider: refreshToken(authentication)
  AuthService-->>AuthController: access token + refresh token + roles
  AuthController-->>Client: 200/201 AuthResponse
```

### Authenticated property operations

```mermaid
sequenceDiagram
  autonumber
  actor Client
  participant JwtFilter as JwtAuthenticationFilter
  participant TokenProvider as JwtTokenProvider
  participant UserDetails as CustomUserDetailsService
  participant Controller as PropertyController
  participant Service as PropertyService
  participant CurrentUser as CurrentUserService
  participant ImageStore as ImageStorageService
  participant Repository as PropertyRepository
  participant Database as PostgreSQL

  Client->>JwtFilter: Request with Bearer access token
  JwtFilter->>TokenProvider: validAccess(token)
  TokenProvider-->>JwtFilter: true + email subject
  JwtFilter->>UserDetails: loadUserByUsername(email)
  UserDetails-->>JwtFilter: authorities
  JwtFilter->>Controller: continue secured request

  alt OWNER creates a property
    Controller->>Service: create(validated form, images)
    Service->>CurrentUser: get()
    CurrentUser-->>Service: authenticated owner
    loop Each image
      Service->>ImageStore: store(file)
      ImageStore-->>Service: image URL
    end
    Service->>Repository: save(property)
    Repository->>Database: INSERT property and image rows
    Service-->>Controller: PropertyResponse
  else Owner updates/deletes/changes availability
    Controller->>Service: operation(propertyId)
    Service->>Repository: findById(propertyId)
    Service->>CurrentUser: get()
    alt Authenticated user is creator
      Service->>Repository: save/delete
      Service-->>Controller: response or 204
    else Different owner
      Service-->>Controller: AccessDeniedException
      Controller-->>Client: 403 consistent error JSON
    end
  else User views property details
    Controller->>Service: details(propertyId)
    Service->>Repository: incrementViews(propertyId)
    Repository->>Database: atomic views = views + 1
    Service->>Repository: findById(propertyId)
    Service-->>Controller: PropertyResponse
  else User searches
    Controller->>Service: search(filters, pageable)
    Service->>Repository: findAll(JPA Specification, pageable)
    Repository->>Database: filtered, sorted, paginated SELECT
    Service-->>Controller: Page of PropertyResponse
  end
  Controller-->>Client: HTTP response
```

### Contact request approval and email delivery

```mermaid
sequenceDiagram
  autonumber
  actor Tenant
  actor Owner
  participant Controller as ContactRequestController
  participant Service as ContactRequestService
  participant CurrentUser as CurrentUserService
  participant PropertyRepo as PropertyRepository
  participant ContactRepo as ContactRequestRepository
  participant Email as SmtpEmailService
  participant SMTP as SMTP or Amazon SES

  Tenant->>Controller: POST /api/v1/contact-requests {propertyId}
  Controller->>Service: create(request)
  Service->>CurrentUser: get()
  CurrentUser-->>Service: tenant
  Service->>PropertyRepo: findById(propertyId)
  PropertyRepo-->>Service: property + owner
  Service->>ContactRepo: existsByTenantIdAndPropertyId(...)
  alt First valid request
    Service->>ContactRepo: save(status=PENDING)
    Service-->>Tenant: 201 PENDING
  else Duplicate or own property
    Service-->>Tenant: 400 business error
  end

  Owner->>Controller: GET /api/v1/contact-requests
  Controller->>Service: ownerRequests(pageable)
  Service->>ContactRepo: findByPropertyOwnerId(ownerId, pageable)
  ContactRepo-->>Owner: requests for owned properties only

  Owner->>Controller: PATCH /api/v1/contact-requests/{id}/approve
  Controller->>Service: approve(requestId)
  Service->>ContactRepo: findById(requestId)
  Service->>CurrentUser: get()
  alt Owner owns the requested property and status is PENDING
    Service->>ContactRepo: save(status=APPROVED)
    Service-)Email: sendApprovedContact(request) asynchronously
    Email->>SMTP: Send owner name, email, and phone
    SMTP-->>Tenant: Approval email
    Service-->>Owner: 200 APPROVED
  else Wrong owner
    Service-->>Owner: 403 Forbidden
  else Already processed
    Service-->>Owner: 400 business error
  end
```

The same ownership check is used for rejection; rejection changes the state to `REJECTED` and does not send an email.

## Prerequisites and local setup

- JDK 21
- Maven 3.9+
- Docker Desktop

Recommended local development flow: run PostgreSQL in Docker and run the Spring Boot API with Maven on your machine.

Start PostgreSQL only:

```powershell
cd D:\JAVA\prop-ease
docker compose up -d postgres
```

Then run the API locally with Maven:

```powershell
cd D:\JAVA\prop-ease
mvn test
mvn spring-boot:run
```

If your default Java is older than 21, point the terminal to a newer JDK before running Maven. For example, on this machine JDK 22 is available:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-22'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

For this Maven-local mode, the app reads `D:\JAVA\prop-ease\application-override.yaml`. The default local database connection is:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/rentaldb
    username: postgres
    password: postgres
```

Use `localhost` here because the Spring Boot app is running on your machine while PostgreSQL is exposed from Docker on host port `5433`. Inside Docker, PostgreSQL still listens on container port `5432`.

Flyway automatically creates all tables, indexes, constraints, and the three role records. Hibernate is set to `validate`, so schema drift fails fast.

## Docker

Full Docker mode is also available for later deployment-style local testing. In this mode both the app and PostgreSQL run inside Docker, so the app uses `postgres` as the database host instead of `localhost`.

```powershell
Copy-Item .env.example .env
# Replace JWT_SECRET in .env
docker compose up --build -d
docker compose logs -f app
docker compose down
```

PostgreSQL is exposed on host port `5433` and the API on `8080`. Database and image volumes persist across restarts.

## Authentication

Register with role names without the `ROLE_` prefix:

```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "Password@123",
  "roles": ["OWNER", "TENANT"]
}
```

Use the returned access token as `Authorization: Bearer <token>`. Access tokens default to 15 minutes and refresh tokens to seven days; both are configurable through environment variables. Refresh tokens are type-bound and cannot authenticate API requests. Changing a password or disabling a user should be paired with a production token-revocation strategy if immediate invalidation is required.

## API documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

When the application reaches the ready state, it prints the Swagger UI URL, OpenAPI JSON URL, and a sorted catalog of every application endpoint to the startup log. The catalog includes each route's HTTP method, path, controller, and handler method. Springdoc independently discovers the same controller routes and displays them in Swagger UI.

```text
================ PROP EASE API ================
Swagger UI : http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
API endpoints:
  POST          /api/v1/auth/login  -> AuthController.login
  GET           /api/v1/properties/search  -> PropertyController.search
  ...
================================================
```

| Method | Path | Authorization |
|---|---|---|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| POST | `/api/v1/auth/refresh` | Public |
| POST | `/api/v1/properties` | OWNER |
| PUT / DELETE | `/api/v1/properties/{id}` | Creating owner |
| GET | `/api/v1/properties/my` | OWNER |
| GET | `/api/v1/properties/{id}` | Authenticated; increments views atomically |
| GET | `/api/v1/properties/search` | Authenticated; paginated and sortable |
| PATCH | `/api/v1/properties/{id}/availability` | Creating owner |
| GET | `/api/v1/properties/{id}/analytics` | Creating owner |
| POST | `/api/v1/contact-requests` | TENANT |
| GET | `/api/v1/contact-requests` | OWNER; own properties only |
| PATCH | `/api/v1/contact-requests/{id}/approve` | Owning OWNER; sends email |
| PATCH | `/api/v1/contact-requests/{id}/reject` | Owning OWNER |

Property create/update uses `multipart/form-data` with the documented scalar fields and zero or more `images` files. Search supports `city`, `area`, `rentMin`, `rentMax`, `bedrooms`, `bathrooms`, `availability`, `postedAfter`, `page`, `size`, and `sort`, for example:

```text
GET /api/v1/properties/search?city=Hyderabad&area=Kondapur&bedrooms=2&bathrooms=2&rentMax=30000&page=0&size=10&sort=rentAmount,asc
```

## Configuration

No production secret is committed. Important environment variables are `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `IMAGE_DIRECTORY`, and `IMAGE_BASE_URL`.

### External override file

The base `src/main/resources/application.yml` imports the external override file through `APP_OVERRIDE_FILE`. If the variable is not set, it defaults to the local project file at `D:/JAVA/prop-ease/application-override.yaml`:

```yaml
spring:
  config:
    import: optional:file:${APP_OVERRIDE_FILE:D:/JAVA/prop-ease/application-override.yaml}
```

Values in `application-override.yaml` override matching base values. Because the import is optional, the packaged application can still start when the file is absent. Keep local or environment-specific changes in this override file instead of editing the base configuration. The override is excluded from Git so credentials are not accidentally committed.

For IntelliJ/local Maven, no extra setting is required when the project lives at `D:\JAVA\prop-ease`. If you move the project, set this environment variable in the IntelliJ Run Configuration or terminal:

```powershell
cd D:\JAVA\prop-ease
$env:APP_OVERRIDE_FILE='D:/JAVA/prop-ease/application-override.yaml'
mvn spring-boot:run

# Packaged JAR—the same external override is loaded
java -jar target\prop-ease-1.0.0.jar
```

Docker Compose mounts `application-override.yaml` into `/app/application-override.yaml` as read-only and sets `APP_OVERRIDE_FILE=/app/application-override.yaml`, so the same override mechanism works inside the container.

## Tests and coverage

```powershell
mvn clean verify
```

The suite contains security-token unit tests, service tests with Mockito, controller validation tests with MockMvc, and JaCoCo reporting at `target/site/jacoco/index.html`. Add tests with every behavior change and maintain at least 70% line coverage in CI. `mvn test` runs the current tests; `mvn clean verify` also enforces the configured coverage gate.

Code style is enforced during Maven's `validate` phase. Spotless applies Google Java Format and removes unused imports; Checkstyle rejects wildcard and unused imports.

```powershell
# Reformat source and test code
mvn spotless:apply

# Verify formatting and imports without changing files
mvn spotless:check checkstyle:check
```

## AWS deployment

1. Create private subnets and security groups. Permit the application security group to reach RDS on 5432; do not expose RDS publicly.
2. Create RDS PostgreSQL with encryption, automated backups, Multi-AZ for production, and credentials stored in Secrets Manager. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from those secrets.
3. Build the image, push it to ECR, and run it on EC2 (or ECS) behind an HTTPS Application Load Balancer. Give the instance/task an IAM role instead of static AWS keys. Store `JWT_SECRET` in Secrets Manager or SSM Parameter Store.
4. Create a private S3 bucket with block-public-access, encryption, lifecycle rules, and CloudFront or presigned URLs. Implement the existing `ImageStorageService` with AWS SDK v2; persist only the resulting object URL/key.
5. Verify an SES domain/address, move the account out of the sandbox, grant only `ses:SendEmail`, and supply the SES SMTP endpoint and credentials through the SMTP environment variables. Use TLS on port 587.
6. Run Flyway during a single controlled deployment task before scaling out. Configure health checks, CloudWatch logs/alarms, WAF, backups, least-privilege IAM, and rolling or blue/green deployments.

## Production notes

- Put the API behind TLS and configure an explicit CORS allow-list.
- Replace local image storage with S3 and add malware/content validation.
- Consider refresh-token rotation with hashed token persistence for logout and revocation.
- Rate-limit authentication, property views, and contact requests.
- Use a queue/outbox for reliable email delivery at scale.
