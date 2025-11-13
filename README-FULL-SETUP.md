# Full Setup Guide - Guacamole + Keycloak Together

This guide shows how to run both Guacamole and Keycloak together in a single docker-compose setup.

## Quick Start

### Option 1: Automated Setup (Recommended)

```bash
# Build extension and start all services
./start-full.sh
```

This will:
1. Build the OpenID extension
2. Create deployment directory
3. Copy extension JAR and user-mapping.xml
4. Start Keycloak, PostgreSQL, Guacd, and Guacamole together

### Option 2: Manual Setup

```bash
# 1. Build extension
cd extensions/guacamole-auth-sso
mvn clean package -DskipTests
cd ../..

# 2. Create deployment directory
mkdir -p deployment/extensions

# 3. Copy extension JAR
cp extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/target/guacamole-auth-sso-openid-*.jar \
   deployment/extensions/

# 4. Copy user-mapping.xml
cp user-mapping-example.xml deployment/user-mapping.xml

# 5. Create guacamole.properties
cat > deployment/guacamole.properties <<EOF
openid-authorization-endpoint: http://localhost:8090/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: http://keycloak:8080/realms/myrealm/protocol/openid-connect/certs
openid-issuer: http://localhost:8090/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://localhost:8080/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile
guacd-hostname: guacd
guacd-port: 4822
EOF

# 6. Start all services
docker compose -f docker-compose-full.yml up -d
```

## Services

The `docker-compose-full.yml` includes:

1. **PostgreSQL** - Database for Keycloak
2. **Keycloak** - Identity provider (port 8090)
3. **Guacd** - Guacamole proxy daemon (port 4822)
4. **Guacamole** - Web application (port 8080)

All services are on the same Docker network (`guacamole-network`) so they can communicate internally.

## Configuration Notes

### Keycloak URLs in guacamole.properties

- **Authorization endpoint**: Use `localhost:8090` (browser redirects)
- **JWKS endpoint**: Use `keycloak:8080` (internal Docker network - faster)
- **Issuer**: Use `localhost:8090` (must match what Keycloak puts in tokens)
- **Redirect URI**: Use `localhost:8080` (where browser returns)

Example:
```properties
openid-authorization-endpoint: http://localhost:8090/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: http://keycloak:8080/realms/myrealm/protocol/openid-connect/certs
openid-issuer: http://localhost:8090/realms/myrealm
openid-redirect-uri: http://localhost:8080/
```

## Access Points

- **Keycloak Admin Console**: http://localhost:8090
  - Username: `admin`
  - Password: `admin`

- **Guacamole Web Interface**: http://localhost:8080

## Setup Keycloak

After services start, configure Keycloak:

1. Access http://localhost:8090
2. Log in with admin/admin
3. Create realm (e.g., `myrealm`)
4. Create client `guacamole-client`
5. Create roles: `admin`, `console_accesser`
6. Create test user and assign roles
7. Configure role mapper (Token Claim Name: `groups`)

See `KEYCLOAK-SETUP.md` for detailed steps.

## Verify Everything is Working

```bash
# Check all services
docker compose -f docker-compose-full.yml ps

# Check Keycloak logs
docker compose -f docker-compose-full.yml logs -f keycloak

# Check Guacamole logs
docker compose -f docker-compose-full.yml logs -f guacamole

# Check extension loaded
docker compose -f docker-compose-full.yml logs guacamole | grep "OpenID Authentication Extension"

# Check user-mapping.xml read
docker compose -f docker-compose-full.yml logs guacamole | grep "Reading user mapping file"
```

## Stop All Services

```bash
# Stop all services
docker compose -f docker-compose-full.yml down

# Stop and remove volumes (deletes Keycloak data)
docker compose -f docker-compose-full.yml down -v
```

## Advantages of Combined Setup

1. **Single command** - Start everything with one command
2. **Internal network** - Services can communicate via Docker network (faster)
3. **Dependency management** - Docker Compose handles startup order
4. **Easier testing** - Everything runs together for development

## Network Communication

- **Browser → Keycloak**: `localhost:8090` (external)
- **Browser → Guacamole**: `localhost:8080` (external)
- **Guacamole → Keycloak**: `keycloak:8080` (internal Docker network)
- **Guacamole → Guacd**: `guacd:4822` (internal Docker network)

## Troubleshooting

### Keycloak Not Ready

```bash
# Check Keycloak health
docker compose -f docker-compose-full.yml exec keycloak curl http://localhost:8080/health/ready

# Check Keycloak logs
docker compose -f docker-compose-full.yml logs keycloak
```

### Guacamole Can't Reach Keycloak

1. Verify both are on same network: `docker network inspect guacamole-client_guacamole-network`
2. Check Keycloak is healthy: `docker compose -f docker-compose-full.yml ps`
3. Verify JWKS endpoint uses `keycloak:8080` (internal) not `localhost:8090`

### Port Conflicts

If ports 8080, 8090, or 4822 are already in use:

1. Stop conflicting services, or
2. Modify port mappings in `docker-compose-full.yml`:
   ```yaml
   ports:
     - "8081:8080"  # Change external port
   ```

## File Structure

```
.
├── docker-compose-full.yml    # Combined setup
├── docker-compose.yml          # Guacamole only
├── docker-compose-keycloak.yml # Keycloak only
├── start-full.sh               # Automated full setup
├── deployment/
│   ├── extensions/
│   │   └── guacamole-auth-sso-openid-*.jar
│   ├── user-mapping.xml
│   └── guacamole.properties
└── ...
```

