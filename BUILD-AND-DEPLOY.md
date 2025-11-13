# Build and Deploy Guide

Quick reference for building the extension and deploying to Docker.

## Quick Build

```bash
# Navigate to the extension directory
cd extensions/guacamole-auth-sso

# Build the extension
mvn clean package

# The JAR will be at:
# modules/guacamole-auth-sso-openid/target/guacamole-auth-sso-openid-*.jar
```

## File Locations Summary

### Extension JAR
- **Location**: `GUACAMOLE_HOME/extensions/guacamole-auth-sso-openid-*.jar`
- **In Docker**: `/etc/guacamole/extensions/guacamole-auth-sso-openid-*.jar`

### user-mapping.xml
- **Location**: `GUACAMOLE_HOME/user-mapping.xml`
- **In Docker**: `/etc/guacamole/user-mapping.xml`
- **GUACAMOLE_HOME** is determined by (in order):
  1. System property: `-Dguacamole.home=/path`
  2. Environment variable: `GUACAMOLE_HOME=/path`
  3. `~/.guacamole/`
  4. `/etc/guacamole/`

## Docker Quick Start

### 1. Prepare Files

```bash
# Create deployment directory
mkdir -p deployment/extensions
mkdir -p deployment

# Copy extension JAR
cp extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/target/guacamole-auth-sso-openid-*.jar \
   deployment/extensions/

# Copy user-mapping.xml
cp user-mapping-example.xml deployment/user-mapping.xml

# Create guacamole.properties (if needed)
cat > deployment/guacamole.properties <<EOF
openid-authorization-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs
openid-issuer: https://keycloak.example.com/auth/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://localhost:8080/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile
guacd-hostname: guacd
guacd-port: 4822
EOF
```

### 2. Run with Docker Compose

```bash
# Create docker-compose.yml
cat > docker-compose.yml <<EOF
version: '3.8'
services:
  guacamole:
    image: guacamole/guacamole:latest
    environment:
      - GUACAMOLE_HOME=/etc/guacamole
      - GUACD_HOSTNAME=guacd
      - GUACD_PORT=4822
    volumes:
      - ./deployment:/etc/guacamole:ro
    ports:
      - "8080:8080"
    depends_on:
      - guacd

  guacd:
    image: guacamole/guacd:latest
    ports:
      - "4822:4822"
EOF

# Start services
docker compose up -d

# Check logs
docker compose logs -f guacamole
```

### 3. Verify Deployment

```bash
# Check extension loaded
docker compose logs guacamole | grep "OpenID Authentication Extension"

# Check user-mapping.xml read
docker compose logs guacamole | grep "Reading user mapping file"

# Check role verification (after login)
docker compose logs guacamole | grep "has required role"
```

## Testing Role-Based Access

1. **User with `admin` role**: Should see connections from `user-mapping.xml`
2. **User with `console_accesser` role**: Should see connections from `user-mapping.xml`
3. **User without roles**: Should NOT see any connections

See `KEYCLOAK-ROLES-SETUP.md` for Keycloak configuration.

