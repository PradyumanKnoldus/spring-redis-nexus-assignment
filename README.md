# Employee Library and Redis Application

A Maven assignment demonstrating a reusable employee Java library published to a local Nexus Maven hosted repository and consumed by a Spring Boot REST application. The application stores employee records in MySQL and uses Redis to cache individual employee lookups.

## 1. Project Overview

This workspace contains two independent Maven projects:

- `employee-library`: a dependency-free Java 17 JAR with an `Employee` model and reusable employee utility methods.
- `employee-redis-app`: a Spring Boot 3.5.16 REST API that uses the library, persists employee data with Spring Data JPA and MySQL, and caches employee-by-ID responses in Redis.

The library is published as `com.nashtech:employee-library:1.0.0`. The application resolves that artifact from the local Nexus repository configured in its POM.

## 2. Architecture

```text
Postman / HTTP client
        |
        v
EmployeeController
        |
        v
EmployeeService ----------------------> Employee Library
        |                                 Employee model and utilities
        +---- EmployeeRepository -------------------> MySQL
        |
        +---- Spring Cache annotations ------------> Redis
```

The REST application uses request and response DTOs at the API boundary, maps persisted JPA entities to the library's `Employee` model, and uses the library to validate salary and generate employee codes. MySQL is the system of record; Redis is a short-lived cache for individual employee reads.

## 3. Technologies Used

- Java 17
- Maven
- Spring Boot 3.5.16
- Spring Web and Jakarta Bean Validation
- Spring Data JPA and Hibernate
- MySQL Connector/J
- Spring Data Redis and Spring Cache
- Redis
- Nexus Repository Manager (Maven hosted releases repository)
- JUnit 5, Mockito, and Spring Boot Test

## 4. Prerequisites

Install or provide the following before running the full application:

- JDK 17
- Maven 3.6.3 or later
- MySQL Server, reachable at the configured JDBC URL
- Docker, if using the Redis container command below, or a compatible Redis server
- Nexus Repository Manager at `http://localhost:8081` with the hosted repository `employee-maven-releases`, when publishing or resolving the library through Nexus

The application requires `DB_USERNAME` and `DB_PASSWORD` in its environment. Maven credentials for Nexus, if required, belong in the user's Maven `settings.xml`, not in source control.

## 5. Project Structure

```text
.
├── employee-library/
│   ├── pom.xml
│   ├── README.md
│   └── src/main/java/com/nashtech/employeelibrary/
│       ├── model/Employee.java
│       └── service/EmployeeUtility.java
├── employee-redis-app/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nashtech/employeeredisapp/
│       │   ├── config/RedisCacheConfig.java
│       │   ├── controller/EmployeeController.java
│       │   ├── dto/EmployeeRequest.java
│       │   ├── dto/EmployeeResponse.java
│       │   ├── entity/EmployeeEntity.java
│       │   ├── exception/
│       │   ├── repository/EmployeeRepository.java
│       │   ├── service/EmployeeService.java
│       │   └── EmployeeRedisApplication.java
│       ├── main/resources/application.properties
│       └── test/java/com/nashtech/employeeredisapp/
└── screenshots/
```

## 6. Employee Library

The library is a plain Maven JAR, not a Spring Boot application. It has no Spring Boot dependencies.

Coordinates:

```text
groupId:    com.nashtech
artifactId: employee-library
version:    1.0.0
packaging:  jar
```

`com.nashtech.employeelibrary.model.Employee` is a mutable POJO with `Long id`, `String name`, `String email`, `String department`, and `Double salary`, including no-argument and all-argument constructors, getters, setters, and `toString()`.

`com.nashtech.employeelibrary.service.EmployeeUtility` provides:

- `generateEmployeeCode(Long id)`: returns `EMP-{id}`.
- `isValidSalary(Double salary)`: returns true when the value is non-null and greater than or equal to zero.

The application uses these utilities to generate the API's `employeeCode` and to validate salary in the service layer. Request DTO validation also rejects a missing or negative salary before the service operation proceeds.

## 7. Nexus Repository Configuration

The library POM declares the release deployment target:

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <url>http://localhost:8081/repository/employee-maven-releases/</url>
    </repository>
</distributionManagement>
```

The application POM declares the same Nexus URL as a dependency repository, with repository id `employee-maven-releases`. Nexus must be running locally and the hosted repository must already exist. Repository ids identify Maven settings entries; the deployment id and consumer repository id in this project differ.

## 8. Publish `employee-library` to Nexus

1. Ensure Nexus is running and `employee-maven-releases` is a Maven hosted release repository.
2. If Nexus requires authentication, add credentials to `~/.m2/settings.xml` on Linux/macOS or `%USERPROFILE%\.m2\settings.xml` on Windows. Use the deployment repository id exactly:

   ```xml
   <settings>
       <servers>
           <server>
               <id>nexus-releases</id>
               <username>YOUR_NEXUS_USERNAME</username>
               <password>YOUR_NEXUS_PASSWORD</password>
           </server>
       </servers>
   </settings>
   ```

   Do not commit real credentials. If Nexus also requires authentication to download artifacts, add another `<server>` entry with id `employee-maven-releases` for the consumer's repository id.
3. From the repository root, publish the release:

   ```bash
   cd employee-library
   mvn clean deploy
   ```

The uploaded coordinates are `com.nashtech:employee-library:1.0.0`. Maven's deploy lifecycle builds the JAR and uploads it to the configured distribution management repository. The same command can be used on Windows from PowerShell with `Set-Location employee-library`.

For a local-only development build that does not publish to Nexus, use `mvn clean install` in `employee-library`; this installs the artifact into the local Maven repository.

## 9. How `employee-redis-app` Consumes the Library

The app POM declares:

```xml
<dependency>
    <groupId>com.nashtech</groupId>
    <artifactId>employee-library</artifactId>
    <version>1.0.0</version>
</dependency>
```

When resolving this dependency, Maven checks its local repository and the app POM's configured Nexus repository. Publish the library to Nexus first, or install it locally for local-only development. If Nexus needs read credentials, the matching `<server>` id is `employee-maven-releases`.

The app converts between JPA entities, the reusable library model, and API DTOs. Its response includes the generated employee code.

## 10. MySQL Setup

Create the database and an application account in MySQL. For example, execute the following as a MySQL administrator and replace the sample password with a secure local value:

```sql
CREATE DATABASE employee_db;
CREATE USER 'employee_app'@'localhost' IDENTIFIED BY 'change-this-password';
GRANT ALL PRIVILEGES ON employee_db.* TO 'employee_app'@'localhost';
```

Set environment variables for the process that will run the application:

PowerShell:

```powershell
$env:DB_USERNAME = "employee_app"
$env:DB_PASSWORD = "change-this-password"
```

Linux/macOS shell:

```bash
export DB_USERNAME=employee_app
export DB_PASSWORD='change-this-password'
```

The configured database is `employee_db` on `localhost:3306`. Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so it creates or updates the mapped `employees` table at application startup. Use a persistent database for data that needs to survive restarts.

## 11. Redis Docker Setup

There is no Docker Compose or Dockerfile in this workspace. To start a local Redis instance with Docker and expose the configured port:

```bash
docker run --name employee-redis -d -p 6379:6379 redis:7-alpine
```

The application is configured to connect to Redis at `localhost:6379`. Confirm the container is running with `docker ps`. To stop it, run `docker stop employee-redis`; to start the same container later, run `docker start employee-redis`.

## 12. Application Configuration

Configuration is in `employee-redis-app/src/main/resources/application.properties`:

| Property | Configured value |
| --- | --- |
| HTTP port | `8080` |
| JDBC URL | `jdbc:mysql://localhost:3306/employee_db` |
| DB username/password | `DB_USERNAME` / `DB_PASSWORD` environment variables |
| Hibernate schema behavior | `update` |
| Redis host/port | `localhost:6379` |
| Cache provider | Redis |
| Cache name | `employees` |

Database, Redis, and server values can be overridden with Spring Boot's normal external configuration mechanisms. Do not put database passwords or Nexus credentials in committed files.

## 13. REST API Documentation

Base URL: `http://localhost:8080/api/employees`

| Method | Path | Behavior | Success response |
| --- | --- | --- | --- |
| `POST` | `/api/employees` | Create an employee | `201 Created`, employee JSON, and a `Location` header |
| `GET` | `/api/employees` | List all employees | `200 OK`, JSON array |
| `GET` | `/api/employees/{id}` | Retrieve one employee | `200 OK`, employee JSON |
| `PUT` | `/api/employees/{id}` | Replace employee fields | `200 OK`, updated employee JSON |
| `DELETE` | `/api/employees/{id}` | Delete an employee | `204 No Content` |

Create and update request body:

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "department": "Engineering",
  "salary": 85000.0
}
```

Name and department must be nonblank, email must be valid, and salary must be present and at least zero. Employee IDs are generated by the database. Employee responses contain `id`, `employeeCode`, `name`, `email`, `department`, and `salary`.

A missing employee returns `404 Not Found`. Invalid input returns `400 Bad Request`. Error bodies contain `timestamp`, `status`, `message`, and `path`.

## 14. Redis Caching Explanation

Redis is configured as the Spring Cache provider. The cache manager registers the `employees` cache, serializes `EmployeeResponse` values as JSON, and assigns entries a 60-second TTL. The cache key for single-record operations is the employee ID.

`GET /api/employees` is not cached; it reads from the repository. Caching applies to service calls for `getEmployeeById` only, with create/update/delete keeping the corresponding single-employee cache entry in sync.

## 15. `@Cacheable` Behavior

`getEmployeeById(id)` uses `@Cacheable` with cache name `employees` and key `id`. On a cache hit, Spring returns the cached response without querying the repository. On a miss, the service loads the employee from MySQL and stores its response. Missing employees throw an exception and are not stored as cached null values.

## 16. `@CachePut` Behavior

`createEmployee(request)` uses `@CachePut` to place the created response in `employees` under its generated ID. `updateEmployee(id, request)` uses `@CachePut` with the supplied ID, replacing that key's value with the saved response. Unlike `@Cacheable`, `@CachePut` always invokes the method before updating the cache.

## 17. `@CacheEvict` Behavior

`deleteEmployee(id)` uses `@CacheEvict` to remove the matching `employees` entry after a successful delete operation. The next lookup will load from the repository and return not found for the deleted record.

## 18. TTL Configuration

The TTL is currently fixed at 60 seconds in `RedisCacheConfig`, using `entryTtl(Duration.ofSeconds(60))`. After expiration, a later employee-by-ID request is a cache miss and loads the current value from MySQL. To change the TTL, update this configuration and rebuild the application.

## 19. Run the Application

Start MySQL and Redis, configure `DB_USERNAME` and `DB_PASSWORD`, and make sure `employee-library:1.0.0` is available to Maven via Nexus or the local Maven repository. Then run:

```bash
cd employee-redis-app
mvn spring-boot:run
```

The API listens at `http://localhost:8080`. Stop it with `Ctrl+C` in the running terminal.

## 20. Run Tests

Run the application test suite from its project directory:

```bash
cd employee-redis-app
mvn test
```

The tests cover controller HTTP responses and validation, employee service behavior, and cache hit/update/eviction behavior. The caching unit tests use an in-memory Spring `ConcurrentMapCacheManager`; they do not require a running Redis instance. Controller tests use MockMvc. The library currently has no separate test source set.

## 21. Postman Testing Examples

Create a request in Postman for each method below. For `POST` and `PUT`, select **Body → raw → JSON** and set `Content-Type: application/json`.

Create an employee:

```http
POST http://localhost:8080/api/employees
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "department": "Engineering",
  "salary": 85000.0
}
```

Use the returned `id` for the remaining examples:

```http
GET http://localhost:8080/api/employees
GET http://localhost:8080/api/employees/1
```

Update an employee:

```http
PUT http://localhost:8080/api/employees/1
Content-Type: application/json

{
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "department": "Product",
  "salary": 91000.0
}
```

Delete an employee:

```http
DELETE http://localhost:8080/api/employees/1
```

Expected successful status codes are `201`, `200`, `200`, `200`, and `204`, respectively. To verify error handling, request an unknown ID such as `GET /api/employees/404`, or submit a negative salary; expected statuses are `404` and `400`.

## 22. Troubleshooting

- **Maven cannot resolve `employee-library:1.0.0`:** Confirm it was deployed to the configured Nexus repository or installed locally with `mvn clean install`; verify the coordinates and repository URL. Use `mvn -U` to retry dependency resolution.
- **Nexus returns 401/403:** Check the credentials and permissions in Maven `settings.xml`. The deployment server id is `nexus-releases`; the app's dependency repository id is `employee-maven-releases`.
- **Nexus returns 404:** Confirm Nexus is reachable at port `8081`, the hosted repository name is `employee-maven-releases`, and the artifact was successfully published.
- **Database connection fails:** Confirm MySQL is running, `employee_db` exists, the account has privileges, port `3306` is reachable, and both environment variables are set in the same shell used to launch the app.
- **Redis connection fails:** Confirm Redis is running and reachable on `localhost:6379`; inspect the container with `docker ps` and `docker logs employee-redis`.
- **Port is already in use:** Stop the process using port `8080`, `3306`, `6379`, or `8081`, or configure the corresponding service to use another port and update the application properties.
- **Request returns 400:** Send valid JSON and ensure name, email, department, and non-negative salary satisfy the request constraints.

## 23. Screenshots and Evidence

The `screenshots/` directory contains the assignment evidence:

- Library build: [01-library-build.png](screenshots/01-library-build.png)
- Nexus repository: [02-nexus-repository.png](screenshots/02-nexus-repository.png)
- Library published: [03-library-published.png](screenshots/03-library-published.png)
- Nexus consumption: [04-nexus-consumption.png](screenshots/04-nexus-consumption.png)
- Create employee: [05-crud-api-POST.png](screenshots/05-crud-api-POST.png)
- List employees: [06-crud-api-GetAll.png](screenshots/06-crud-api-GetAll.png)
- Get employee by ID: [07-crud-api-GetById.png](screenshots/07-crud-api-GetById.png)
- Update employee: [08-crud-api-PUT.png](screenshots/08-crud-api-PUT.png)
- Delete employee: [09-crud-api-DELETE.png](screenshots/09-crud-api-DELETE.png)
- Cache lookup: [10-cacheable.png](screenshots/10-cacheable.png)
- Cache update: [11-cache-put.png](screenshots/11-cache-put.png)
- Cache eviction: [12-cache-evict.png](screenshots/12-cache-evict.png)
- Cache TTL: [13-cache-ttl.png](screenshots/13-cache-ttl.png)

## 24. Assignment Evaluation Mapping

| Assignment area | Implementation or evidence |
| --- | --- |
| Reusable Maven library | `employee-library/pom.xml`, `Employee.java`, `EmployeeUtility.java`; library build evidence in `01-library-build.png` |
| Nexus hosted release publishing | Library `distributionManagement`; repository and publication evidence in `02-nexus-repository.png` and `03-library-published.png` |
| Application dependency consumption | `employee-redis-app/pom.xml`; evidence in `04-nexus-consumption.png` |
| Employee REST CRUD | `EmployeeController`, `EmployeeService`, DTOs, and controller tests; evidence in `05-crud-api-POST.png` through `09-crud-api-DELETE.png` |
| MySQL persistence | `EmployeeEntity`, `EmployeeRepository`, and application datasource configuration |
| Redis caching | `RedisCacheConfig` and service cache annotations; tests in `EmployeeServiceCachingTest`; evidence in `10-cacheable.png` through `13-cache-ttl.png` |
| Validation and error handling | `EmployeeRequest`, `GlobalExceptionHandler`, and controller/service tests |

The screenshots are evidence files included with this submission; they are not automated test output generated by the README instructions.
