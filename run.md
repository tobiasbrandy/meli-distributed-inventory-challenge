## Run the demo

Prerequisites: Docker and Docker Compose.

### 1) Build JARs

```bash
./mvnw -q -DskipTests package
```

### 2) Start the stack

```bash
docker compose up -d
```

Services:

- central: http://localhost:8081
- store1: http://localhost:8082
- store2: http://localhost:8083
- redis: localhost:6379

Health checks:

```bash
curl -s http://localhost:8081/
curl -s http://localhost:8082/
curl -s http://localhost:8083/
```

### 3) Interactive demo via IDE HTTP client (recommended)

Open `demo/demo.http` in VS Code (REST Client) or IntelliJ HTTP client.

- Ensure `demo/http-client.env.json` is present; the default `dev` environment targets localhost ports.
- Run the requests top-to-bottom:
  - Health checks for central, store1, store2
  - Connect stores
  - Create a product on store1 and set quantity
  - Central fetches inventory and processes purchases
  - Insufficient stock case (expects RFC7807 400)
  - Disconnect/reconnect store1 and retry purchase (expects 500 then 200)
  - Local store purchase and final central verification

Assertions are embedded; you will see ✅/❌ for each step.

### 4) Optional: Observe messaging and heartbeats in Redis

Show one recent event in the central stream for store-1:

```bash
docker exec -it redis redis-cli XRANGE $(docker exec -it central sh -lc 'grep -E "^streams\.centralToStore" -r /app | sed -n 1p >/dev/null; echo -n "central:to:store:store-1:stream"') - + COUNT 1
```

Check store heartbeat key (fresh timestamp means alive):

```bash
docker exec -it redis redis-cli GET store:store-1:heartbeat
```

### 5) Logs (optional)

```bash
docker logs central --since 5m | tail -n 100
docker logs store1 --since 5m | tail -n 100
```

### 6) Tear down

```bash
docker compose down -v
```
