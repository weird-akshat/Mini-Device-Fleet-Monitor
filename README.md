# Mini Device Fleet Monitor

A small REST API for registering devices, receiving heartbeats, tracking device connectivity, and viewing fleet-wide status. The application is built with Java, Spring Boot, Spring Data JPA, and PostgreSQL.

## Features

- Register devices using client-provided IDs.
- Receive heartbeats from registered devices.
- Accept a heartbeat only when its reported status is `OK`.
- Record the server-side receipt time of the latest accepted heartbeat.
- Track optional CPU usage and signal-strength metrics.
- Derive `ONLINE` / `OFFLINE` status using a 30-second timeout.
- List all devices or retrieve one device by ID.
- Return a fleet summary containing total, online, and offline counts.
- Validate request fields and handle common API errors.
- Persist device information in PostgreSQL.
- Generate interactive API documentation with Springdoc OpenAPI / Swagger UI.
- Use SLF4J logging through Lombok.

## Architecture

The application follows a layered architecture that separates HTTP handling, business logic, data persistence, and data transformation.

- **Controller:** Exposes REST endpoints and handles incoming HTTP requests.
- **Service:** Coordinates business operations, applies validation and heartbeat rules, and orchestrates repository and mapper calls.
- **Repository:** Uses Spring Data JPA to interact with PostgreSQL.
- **Mapper:** Converts between request DTOs, device entities, and response DTOs.
- **Entity:** Represents the device data persisted in the database.
- **DTOs:** Define the request and response structures exposed through the API.
- **Exception handling:** Centralizes application errors and maps them to appropriate HTTP responses.

The controller delegates business logic to the service. The service interacts with the repository to retrieve or persist entities and uses the mapper to construct response DTOs. Device connectivity is derived from the latest accepted heartbeat when queried rather than stored separately.

## Technology Stack

- Java 17
- Spring Boot 4.1.1
- Maven
- PostgreSQL
- Spring Data JPA / Hibernate
- Jakarta Bean Validation
- Lombok and SLF4J
- Springdoc OpenAPI / Swagger UI
- JUnit, Mockito, and Spring MVC testing support

## Prerequisites

- Java 17 or a compatible JDK
- PostgreSQL
- Git
- Python 3 for the simulator

The repository includes the Maven wrapper, so a separate Maven installation is not required.

Check Java and Python:

```bash
java -version
python3 --version
```

## Database Setup

Create a database and a dedicated user in PostgreSQL. For example, run the following as a PostgreSQL administrator:

```sql
CREATE USER devicemonitor_user WITH PASSWORD 'replace-with-a-local-password';

CREATE DATABASE devicemonitor OWNER devicemonitor_user;
```

Create a `.env` file in the project root:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/devicemonitor
DB_USER=devicemonitor_user
DB_PASSWORD=replace-with-a-local-password
```

The application imports this file through `application.properties`. Ensure `.env` is in `.gitignore` and never commit real credentials.

The current development configuration uses Hibernate schema auto-update. For a production deployment, use versioned database migrations such as Flyway or Liquibase.

## Build and Run

Clone the repository and enter its directory:

```bash
git clone git@github.com:weird-akshat/Mini-Device-Fleet-Monitor.git
cd devicemonitor
```

Configure the `.env` file as described above, then build the project:

```bash
./mvnw clean package
```

Run the automated tests:

```bash
./mvnw test
```

Start the application:

```bash
./mvnw spring-boot:run
```

The application listens on port `8080` by default unless configured otherwise.

## API Documentation

With the application running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Swagger UI can be used to inspect endpoints, submit requests, and inspect responses.

## API Reference

The endpoints below reflect the application's current routes. Most query endpoints use an envelope with `success`, `message`, and `data`; device registration currently returns the registered device directly.

### 1. Register a device

`POST /devices`

Request:

```json
{
  "id": "device-01",
  "name": "Lab Device 01"
}
```

A successful registration returns HTTP `201 Created`. Device IDs are supplied by the client and must be unique.

Example:

```bash
curl -i -X POST http://localhost:8080/devices \
  -H 'Content-Type: application/json' \
  -d '{"id":"device-01","name":"Lab Device 01"}'
```

### 2. Send a heartbeat

`POST /devices/{id}/heartbeat`

Request:

```json
{
  "status": "OK",
  "cpuUsage": 42.0,
  "signalStrength": 71
}
```

Only a request with reported status `OK` is accepted as a heartbeat. An accepted heartbeat updates `lastHeartbeat` using the server's receipt time and updates any metrics supplied in the request.

If the reported status is not `OK` or is missing, the heartbeat is ignored: the timestamp and metrics are not updated. The current controller response still uses the general message `Heartbeat processed`; see Known Limitations.

The client does not send the timestamp used for connectivity calculations. This avoids relying on client clocks that may be inaccurate or unsynchronized.

Example:

```bash
curl -i -X POST http://localhost:8080/devices/device-01/heartbeat \
  -H 'Content-Type: application/json' \
  -d '{"status":"OK","cpuUsage":42.0,"signalStrength":71}'
```

### 3. List all devices

`GET /devices`

Returns all registered devices with their derived connectivity status and recorded metrics.

```bash
curl -i http://localhost:8080/devices
```

### 4. Get one device

`GET /devices/{id}`

```bash
curl -i http://localhost:8080/devices/device-01
```

Returns the requested device. If the device ID does not exist, the API should return HTTP `404 Not Found`.

### 5. Fleet summary

`GET /summary`

Returns the total number of registered devices and the number currently online and offline.

```bash
curl -i http://localhost:8080/summary
```

Example response:

```json
{
  "success": true,
  "message": "Fleet summary retrieved successfully",
  "data": {
    "totalDevices": 5,
    "onlineDevices": 3,
    "offlineDevices": 2
  }
}
```

The counts are illustrative. The summary is calculated from the current device records.

## Connectivity and Timeout Rules

Connectivity is derived from the most recent **accepted** heartbeat:

- **ONLINE:** the latest accepted heartbeat was received less than 30 seconds ago.
- **OFFLINE:** the device has never sent an accepted heartbeat, or the latest accepted heartbeat is 30 seconds old or older.

The status is calculated when device information or the fleet summary is requested. No scheduled task is needed to update device status in the database.

A heartbeat whose reported status is not `OK` does not refresh the timestamp. Therefore, a device that stops sending accepted heartbeats eventually becomes offline.

The server's receipt time is used instead of a timestamp supplied by the device because client clocks can be inaccurate or unsynchronized. This makes the timeout calculation consistent on the server.

## Device Simulator

The Python simulator registers five devices and sends a heartbeat every five seconds. It uses only Python's standard library, so no additional Python packages are required.

### Run the simulator

Start the Spring Boot application first. Open a second terminal at the project root and run:

```bash
python3 simulator/simulator.py
```

To specify a different application URL:

```bash
python3 simulator/simulator.py --base-url http://localhost:8080
```

The simulator attempts to register `device-01` through `device-05`. It then runs a separate heartbeat thread for each device and sends `status: "OK"` with sample CPU and signal-strength values.

If a device is already registered and the API returns HTTP `409`, the simulator reports that it exists and continues.

### Simulator commands

Enter commands at the simulator prompt:

| Command | Description |
|---|---|
| `stop device-01` | Stop heartbeats for that device. |
| `start device-01` | Resume heartbeats for that device. |
| `status` | Show which simulated devices are sending heartbeats. |
| `help` | Show available commands. |
| `quit` | Stop the simulator. |

Replace `device-01` with any of the five simulated device IDs.

### Verify the 30-second timeout

1. Start the application and simulator.
2. Query `GET /devices` and confirm the simulated devices are online.
3. Enter `stop device-01` in the simulator.
4. Wait more than 30 seconds after its last accepted heartbeat.
5. Query `GET /devices/device-01` or `GET /summary`.
6. Confirm the stopped device becomes offline while the other devices remain online.
7. Enter `start device-01` to resume heartbeats.

Stopping one simulated device does not stop the other heartbeat threads. Use `quit` to stop the whole simulator.

If registration or heartbeat requests fail, check that the Spring Boot application is running, the base URL is correct, and the device endpoints accept the documented JSON fields.

## Testing

Run the automated tests with:

```bash
./mvnw test
```

Tests should cover the core behaviors:

- Valid device registration.
- Duplicate device registration.
- Heartbeat handling for registered devices.
- Unknown-device errors.
- Request validation.
- Listing all devices and retrieving a device by ID.
- Fleet summary counts.
- Online/offline status and the 30-second timeout.

Controller tests using `@WebMvcTest` and `@MockitoBean` mock the service layer and verify HTTP behavior. Service tests should separately verify business rules and persistence interactions.

For deterministic timeout tests, an injectable `Clock` or another controllable time source avoids sleeping for 30 seconds in each test.

## Assumptions

1. Device IDs are assigned by clients and uniquely identify registered devices.
2. A device must be registered before it can send a heartbeat.
3. Only heartbeats with reported status `OK` are accepted.
4. The server's receipt time is authoritative for the timeout calculation.
5. CPU usage is a percentage in the range 0–100.
6. Signal strength is currently validated as a value in the range 0–100.
7. Missing optional metrics do not overwrite previously stored metrics.
8. A registered device that has never sent an accepted heartbeat is offline.
9. Fleet summary counts include every registered device, including offline devices.

## Known Limitations

- The application is intended for a small fleet, not high-volume production workloads.
- The list-all endpoint returns every device; pagination would help for larger fleets.
- The summary implementation loads device records before calculating counts. Database-side aggregation would scale better.
- The timeout uses the application server's clock.
- The reported health status is checked but not persisted as a separate field or history.
- An ignored heartbeat currently receives the same general controller response message as a processed heartbeat. A future version should expose the accepted/ignored outcome clearly.
- Hibernate schema auto-update is convenient for development but should be replaced by migrations for production.
- Concurrent registration requests can race at the application level; the database primary-key constraint remains the final uniqueness safeguard.
- Authentication, authorization, rate limiting, alerting, and a monitoring UI are outside the current scope.
- If signal strength is intended to represent dBm, negative values should be supported instead of using a 0–100 validation range.

## What I Would Improve With One Additional Day


1. Add pagination and database-side fleet aggregation.
2. Use Flyway or Liquibase for schema migrations.
3. Improve duplicate-registration handling under concurrent requests.
4. Return an explicit result for accepted versus ignored heartbeats.
5. Add Docker Compose for the application and PostgreSQL.
6. Add health checks and basic operational metrics.
7. Add authentication and authorization if the API is deployed outside a trusted environment.

## AI Usage

- **ChatGPT:** Used for assistance with method implementations and for drafting this README and the device simulator file.
- **GitHub Copilot:** Used to help generate unit tests.

