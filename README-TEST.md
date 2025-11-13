# Quick Test Guide

## Prerequisites

- Docker and Docker Compose installed
- Maven installed (for building the extension)
- Keycloak (can be started with separate docker compose file)

## Quick Start

### 1. Build and Deploy

```bash
# Make the setup script executable
chmod +x test-setup.sh

# Run the setup script
./test-setup.sh
```

The script will:
- Build the OpenID extension
- Create deployment directory structure
- Copy extension JAR and user-mapping.xml
- Start Docker containers

### 2. Start Keycloak (Separate Container)

```bash
# Start Keycloak in separate docker-compose file
docker compose -f docker-compose-keycloak.yml up -d

# Access Keycloak Admin Console
# URL: http://localhost:8090
# Username: admin
# Password: admin
```

See `KEYCLOAK-SETUP.md` for detailed Keycloak configuration steps.

### 3. Configure Keycloak Settings

Edit `deployment/guacamole.properties` and update with your Keycloak configuration:

```properties
openid-authorization-endpoint: https://your-keycloak.com/auth/realms/yourrealm/protocol/openid-connect/auth
openid-jwks-endpoint: https://your-keycloak.com/auth/realms/yourrealm/protocol/openid-connect/certs
openid-issuer: https://your-keycloak.com/auth/realms/yourrealm
openid-client-id: your-client-id
openid-redirect-uri: http://localhost:8080/
```

### 4. Restart Container (if needed)

```bash
docker compose restart guacamole
```

### 5. Access Guacamole

Open browser: http://localhost:8080

## Manual Setup (Alternative)

If you prefer to set up manually:

### 1. Build Extension

```bash
cd extensions/guacamole-auth-sso
mvn clean package -DskipTests
cd ../..
```

### 2. Create Deployment Directory

```bash
mkdir -p deployment/extensions
```

### 3. Copy Files

```bash
# Copy extension JAR
cp extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/target/guacamole-auth-sso-openid-*.jar \
   deployment/extensions/

# Copy user-mapping.xml
cp user-mapping-example.xml deployment/user-mapping.xml

# Create guacamole.properties (see example below)
```

### 4. Start Containers

```bash
docker compose up -d
```

## Verify Deployment

### Check Extension Loaded

```bash
docker compose logs guacamole | grep "OpenID Authentication Extension"
```

Expected output:
```
Extension "OpenID Authentication Extension" loaded.
```

### Check user-mapping.xml Read

```bash
docker compose logs guacamole | grep "Reading user mapping file"
```

Expected output:
```
Reading user mapping file: "/etc/guacamole/user-mapping.xml"
```

### Check Role Verification

After logging in, check logs:

```bash
docker compose logs guacamole | grep "has required role"
```

Expected output (for user with role):
```
User user@example.com has required role (admin: true, console_accesser: false)
```

Or (for user without role):
```
User user@example.com does not have required role (admin or console_accesser). No connections from user-mapping.xml will be provided.
```

## Test Scenarios

### Test 1: User with Admin Role

1. Ensure user has `admin` role in Keycloak
2. Log in to Guacamole
3. Should see all connections from `user-mapping.xml` in ROOT group

### Test 2: User with console_accesser Role

1. Ensure user has `console_accesser` role in Keycloak
2. Log in to Guacamole
3. Should see all connections from `user-mapping.xml` in ROOT group

### Test 3: User without Required Roles

1. Ensure user does NOT have `admin` or `console_accesser` role
2. Log in to Guacamole
3. Should NOT see any connections from `user-mapping.xml`

### Test 4: Update user-mapping.xml

1. Edit `deployment/user-mapping.xml`
2. Add or modify a connection
3. Save the file
4. Check logs - should see "Reading user mapping file" message
5. Refresh Guacamole - new connection should appear (if user has required role)

## Troubleshooting

### Extension Not Loading

```bash
# Check if JAR is in correct location
docker compose exec guacamole ls -la /etc/guacamole/extensions/

# Check container logs
docker compose logs guacamole | grep -i error
```

### user-mapping.xml Not Found

```bash
# Check if file exists in container
docker compose exec guacamole ls -la /etc/guacamole/user-mapping.xml

# Check GUACAMOLE_HOME
docker compose exec guacamole env | grep GUACAMOLE_HOME
```

### Connections Not Showing

1. **Check user roles**: Verify user has `admin` or `console_accesser` role in Keycloak
2. **Check Keycloak mapper**: Ensure roles are in JWT token's `groups` claim
3. **Check logs**: Look for role verification messages
4. **Check user-mapping.xml**: Verify file is valid XML and contains connections

### View All Logs

```bash
# Follow all logs
docker compose logs -f

# Follow only Guacamole logs
docker compose logs -f guacamole

# View last 100 lines
docker compose logs --tail=100 guacamole
```

## Stop and Cleanup

```bash
# Stop containers
docker compose down

# Stop and remove volumes
docker compose down -v

# Remove deployment directory (optional)
rm -rf deployment
```

## Example guacamole.properties

```properties
# OpenID Configuration
openid-authorization-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs
openid-issuer: https://keycloak.example.com/auth/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://localhost:8080/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile

# Guacd Configuration
guacd-hostname: guacd
guacd-port: 4822
```

## File Structure

After setup, your directory should look like:

```
.
├── docker-compose.yml
├── test-setup.sh
├── deployment/
│   ├── extensions/
│   │   └── guacamole-auth-sso-openid-*.jar
│   ├── user-mapping.xml
│   └── guacamole.properties
└── ...
```

