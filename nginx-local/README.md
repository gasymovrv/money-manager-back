# Local Nginx (Development)

This is a simplified local counterpart of the production Nginx setup. It proxies both the frontend (React dev server) and the backend (Spring Boot) running on your host machine through a single local domain.

## Architecture

```
Browser (http://mm.localtest.me)
    |
    v
Nginx in Docker (port 80)
    |
    +-- / (frontend)          --> host.docker.internal:3000 (React dev server in IDE)
    +-- /api/*                --> host.docker.internal:8080 (Spring Boot in IDE)
    +-- /oauth2/*             --> host.docker.internal:8080
    +-- /auth/*               --> host.docker.internal:8080
```

## Why

To mirror the production routing locally while keeping the app running directly from the IDE. This gives you a single domain for both frontend and backend during development.

## Usage

- Start backend in IDE (port 8080)
- Start frontend in IDE (port 3000)
- Start Nginx via Docker Compose from `money-manager-back/`:

```bash
docker-compose -f docker-compose-local.yml up -d
```

### Stop

```bash
docker-compose -f docker-compose-local.yml down
```

### Rebuild after config changes

```bash
docker-compose -f docker-compose-local.yml up -d --build
```

### Logs

```bash
docker-compose -f docker-compose-local.yml logs -f nginx-local
```

## Requirements

- Docker Desktop is running
- Backend is running on localhost:8080
- Frontend is running on localhost:3000

## Notes

- `host.docker.internal` resolves to the host machine from within Docker
- `*.localtest.me` resolves to 127.0.0.1 automatically
- WebSocket headers are forwarded for React fast refresh

## Differences vs production Nginx

| Aspect    | Local                  | Production            |
|-----------|------------------------|-----------------------|
| SSL       | No                     | Yes                   |
| Upstreams | host.docker.internal   | Docker services       |
| Ports     | 80                     | 80, 443               |
| Domain    | mm.localtest.me        | Real production domain |
