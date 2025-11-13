# Quick Start Guide

Complete setup guide for testing Guacamole with Keycloak and user-mapping.xml.

## Option A: Run Everything Together (Recommended for Testing)

```bash
# Build extension and start all services together
./start-full.sh
```

This starts Keycloak, PostgreSQL, Guacd, and Guacamole in one command. See `README-FULL-SETUP.md` for details.

## Option B: Run Separately

### 1. Start Keycloak

```bash
# Start Keycloak and PostgreSQL
docker compose -f docker-compose-keycloak.yml up -d

# Wait for Keycloak to be ready (about 30-60 seconds)
docker compose -f docker-compose-keycloak.yml logs -f keycloak
# Press Ctrl+C when you see "Keycloak is ready"
```

**Access Keycloak Admin Console:**
- URL: http://localhost:8090
- Username: `admin`
- Password: `admin`

### 2. Configure Keycloak

Follow the steps in `KEYCLOAK-SETUP.md` to:
1. Create a realm (e.g., `myrealm`)
2. Create a client (`guacamole-client`)
3. Create roles (`admin`, `console_accesser`)
4. Create a test user
5. Assign roles to user
6. Configure role mapper

**Quick Keycloak Setup:**
1. Go to http://localhost:8090
2. Click "Create Realm" → Name: `myrealm` → Create
3. Go to **Clients** → **Create client**
   - Client ID: `guacamole-client`
   - Client authentication: `Off`
   - Valid redirect URIs: `http://localhost:8080/*`
   - Click **Save**
4. Go to **Realm roles** → **Create role**
   - Create: `admin`
   - Create: `console_accesser`
5. Go to **Users** → **Create new user**
   - Username: `testuser`
   - Email: `test@example.com`
   - Email verified: `ON`
   - Go to **Credentials** tab → Set password
   - Go to **Role mappings** → Assign `admin` role
6. Go to **Clients** → `guacamole-client` → **Client scopes** → `guacamole-client-dedicated` → **Mappers**
   - Click **Add mapper** → **By configuration** → **Realm roles**
   - Name: `realm-roles`
   - Token Claim Name: `groups`
   - Add to ID token: `ON`
   - Multivalued: `ON`
   - Click **Save**

### 3. Build and Deploy Guacamole

```bash
# Run the automated setup script
./test-setup.sh
```

Or manually:

```bash
# Build extension
cd extensions/guacamole-auth-sso
mvn clean package -DskipTests
cd ../..

# Create deployment directory
mkdir -p deployment/extensions

# Copy extension JAR
cp extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/target/guacamole-auth-sso-openid-*.jar \
   deployment/extensions/

# Copy user-mapping.xml
cp user-mapping-example.xml deployment/user-mapping.xml

# Create guacamole.properties
cat > deployment/guacamole.properties <<EOF
openid-authorization-endpoint: http://localhost:8090/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: http://localhost:8090/realms/myrealm/protocol/openid-connect/certs
openid-issuer: http://localhost:8090/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://localhost:8080/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile
guacd-hostname: guacd
guacd-port: 4822
EOF

# Start Guacamole
docker compose up -d
```

### 4. Verify Everything is Working

```bash
# Check Keycloak
docker compose -f docker-compose-keycloak.yml ps

# Check Guacamole
docker compose ps

# Check extension loaded
docker compose logs guacamole | grep "OpenID Authentication Extension"

# Check user-mapping.xml read
docker compose logs guacamole | grep "Reading user mapping file"
```

### 5. Test Login

1. Open http://localhost:8080
2. You should be redirected to Keycloak login
3. Log in with your test user credentials
4. You should be redirected back to Guacamole
5. If user has `admin` or `console_accesser` role, connections from `user-mapping.xml` should be visible

## All-in-One Commands

### Start Everything

```bash
# Terminal 1: Start Keycloak
docker compose -f docker-compose-keycloak.yml up -d

# Wait ~30 seconds for Keycloak to start, then:
# Terminal 2: Build and start Guacamole
./test-setup.sh
```

### Stop Everything

```bash
# Stop Guacamole
docker compose down

# Stop Keycloak
docker compose -f docker-compose-keycloak.yml down
```

### View Logs

```bash
# Guacamole logs
docker compose logs -f guacamole

# Keycloak logs
docker compose -f docker-compose-keycloak.yml logs -f keycloak

# All logs
docker compose logs -f && docker compose -f docker-compose-keycloak.yml logs -f
```

## Troubleshooting

### Keycloak Not Accessible

```bash
# Check if running
docker compose -f docker-compose-keycloak.yml ps

# Check logs
docker compose -f docker-compose-keycloak.yml logs keycloak

# Test health
curl http://localhost:8090/health/ready
```

### Guacamole Can't Connect to Keycloak

1. Verify Keycloak is running: `docker compose -f docker-compose-keycloak.yml ps`
2. Check URLs in `deployment/guacamole.properties` match your Keycloak setup
3. Verify realm and client exist in Keycloak
4. Check Guacamole logs: `docker compose logs guacamole`

### User Can't See Connections

1. Verify user has `admin` or `console_accesser` role in Keycloak
2. Check role mapper is configured correctly (Token Claim Name: `groups`)
3. Check logs: `docker compose logs guacamole | grep "has required role"`
4. Verify `user-mapping.xml` exists and is valid

## Clean Up

```bash
# Stop and remove Guacamole
docker compose down

# Stop and remove Keycloak (keeps data)
docker compose -f docker-compose-keycloak.yml down

# Remove Keycloak and all data
docker compose -f docker-compose-keycloak.yml down -v

# Remove deployment directory
rm -rf deployment
```

## Ports Used

- **Guacamole**: 8080
- **Guacd**: 4822
- **Keycloak**: 8090
- **Keycloak HTTPS**: 8443
- **PostgreSQL**: (internal only)

Make sure these ports are not already in use!

