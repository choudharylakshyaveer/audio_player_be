# Audio Player Backend (`audio_player_be`)

Spring Boot backend for scanning local audio files (MP3/FLAC), storing metadata in MySQL, indexing tracks with Lucene, serving search APIs, playlist APIs, and secure track streaming.

## Tech Stack

- Java 25 (preview enabled)
- Spring Boot 3.5.x
- Spring Data JPA + MySQL
- Apache Lucene 10.x (search indexing/query)
- JWT-based request filter + short-lived stream tokens
- Docker + Docker Compose

## Features

- **Metadata ingestion** from configured folders (`.mp3`, `.flac`).
- **Metadata cleanup/replacement** via configurable text replacement rules.
- **Album and track browsing** APIs.
- **Search support**:
    - multi-part search endpoint (`/search`) for tracks/artists/albums
    - paginated endpoint (`/tracks`) with optional search + sorting
- **Playlist management** APIs (create, add/remove tracks, list playlists).
- **Protected streaming flow**:
    1. client sends app JWT in `Authorization: Bearer <jwt>`
    2. receives short-lived stream token from `/stream/token/{trackId}`
    3. streams `/stream/flac/{trackId}?token=<stream_token>`

---

## Project Structure

```text
src/main/java/org/audio/player
├── controller/        # REST endpoints
├── service/           # business logic (scan/search/playlist/stream)
├── entity/            # JPA entities
├── repository/        # data access + criteria
├── config/            # security/cors/lucene configuration
├── dto/               # API response DTOs
└── utils/             # metadata extractors and streaming utilities
```

---

## Requirements

- **JDK 25** (project compiler release is 25 with preview enabled)
- **Maven 3.9+**
- **MySQL 8+**

> If you use Docker Compose, MySQL and app runtime can be started together.

---

## Configuration

Main config files:

- `src/main/resources/application.yaml` (default; active profile is `prod`)
- `src/main/resources/application-local.yaml`
- `src/main/resources/application-prod.yaml`

### Important Properties

- `server.port` (default `8082`)
- `spring.datasource.*` (MySQL connection)
- `security.jwt.secret` (JWT validation secret)
- `metadata.folders` / `SCANNING_FOLDERS` (comma-separated folders to scan)
- `metadata.replacements.*` (text cleanup rules)
- `cors.allowed-origins`

### Profile Behavior

- `prod` profile uses `SecurityConfigProd` + `JwtFilterProd`.
- non-`prod` profiles use `SecurityConfigLocal` + `JwtFilterLocal`.
- In both cases, `/stream/flac/**` and `OPTIONS` are publicly allowed at filter-chain level; most other routes are validated by JWT filter unless listed in filter skip paths.

---

## Run Locally (Maven)

1. Ensure MySQL is running and credentials in `application-*.yaml` are correct.
2. Start the app:

```bash
mvn spring-boot:run
```

App will be available at:

- `http://localhost:8082`

---

## Run with Docker Compose

1. Create a `.env` file (example):

```env
AUDIO_PLAYER_BE_PORT=8082
SESSION_TIMEOUT=120s
SPRING_APP_NAME=Audio player BE

MYSQL_PORT=3306
MYSQL_DATABASE=audio_player
MYSQL_USER=root
MYSQL_PASSWORD=mysql

SPRING_SECURITY_LOGGING_LEVEL=TRACE
SECURITY_JWT_SECRET=replace_with_a_long_secret_key

# Comma-separated origins (no quotes required)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://100.125.57.120:3000

# Metadata text cleanup
METADATA_REPLACEMENTS_FROM=(DJJOhAL.Com),(RiskyjaTT.CoM)
METADATA_REPLACEMENTS_TO=
```

2. Update host path mappings in `docker-compose.yml` for your machine:
    - `H:/audio_songs:/audio-data/songs:ro`
    - `C:/Users/choud/Downloads/Top 50 Single Track:/audio-data/top50:ro`

3. Start services:

```bash
docker compose up --build -d
```

---

## API Reference

Base URL: `http://localhost:8082`

### Metadata / Library

| Method | Endpoint | Description |
|---|---|---|
| GET | `/saveTrackMetadata` | Scan configured folders, extract metadata, save new tracks, index in Lucene |
| GET | `/albums` | Get distinct albums |
| GET | `/albums?albumName={name}` | Get tracks by album name |
| GET | `/image/{id}` | Get album image/base64 for track id |
| GET | `/column/{columnName}` | Get distinct values for a supported column |
| GET | `/column?columnName={c}&value={v}` | Get tracks filtered by exact column value |

### Search

| Method | Endpoint | Description |
|---|---|---|
| GET | `/search?q={query}` | Aggregated search result (tracks, artists, albums) |
| GET | `/tracks?search=&page=0&size=20&sortBy=id&direction=asc` | Paginated track list, optional Lucene search and sort |

### Playlist

| Method | Endpoint | Description |
|---|---|---|
| POST | `/playlist?name={playlistName}` | Create playlist |
| POST | `/playlist/{playlistId}/addTrack/{trackId}` | Add track to playlist |
| GET | `/playlist` | List playlists |
| GET | `/playlist/{playlistId}` | Get tracks in playlist |
| DELETE | `/playlist/{playlistId}/removeTrack/{trackId}` | Remove track from playlist |

### Streaming

| Method | Endpoint | Description |
|---|---|---|
| POST | `/stream/token/{trackId}` | Issue short-lived stream token (requires valid JWT header) |
| GET | `/stream/flac/{trackId}?token={streamToken}` | Stream FLAC audio if token is valid and not expired |

---

## Example Streaming Flow

```bash
# 1) Request stream token
curl -X POST \
  -H "Authorization: Bearer <APP_JWT>" \
  http://localhost:8082/stream/token/123

# 2) Response
# {"token":"<SHORT_LIVED_STREAM_TOKEN>"}

# 3) Stream track
curl "http://localhost:8082/stream/flac/123?token=<SHORT_LIVED_STREAM_TOKEN>"
```

---

## Notes / Limitations

- Current streaming endpoint is FLAC-specific: `/stream/flac/{trackId}`.
- Stream token secret is hardcoded in `StreamTokenService` (`stream-secret-key`); move this to configuration for production hardening.
- This repository currently has no automated tests under `src/test`.

---

## Build

```bash
mvn clean package
```

Generated artifact: `target/*.jar`