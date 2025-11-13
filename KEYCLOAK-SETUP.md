# Keycloak Setup Guide

This guide explains how to set up Keycloak for testing the Guacamole OpenID extension.

## Quick Start

### 1. Start Keycloak

```bash
docker compose -f docker-compose-keycloak.yml up -d
```

### 2. Access Keycloak Admin Console

- **URL**: http://localhost:8090
- **Username**: `admin`
- **Password**: `admin`

**⚠️ Change these credentials in production!**

## Keycloak Configuration Steps

### Step 1: Create a Realm

1. Log into Keycloak Admin Console
2. Hover over the realm dropdown (top left, shows "master")
3. Click **"Create Realm"**
4. Enter realm name: `myrealm` (or your preferred name)
5. Click **"Create"**

### Step 2: Create a Client for Guacamole

1. In your realm, go to **Clients** → **Create client**
2. **Client type**: `OpenID Connect`
3. **Client ID**: `guacamole-client`
4. Click **"Next"**

5. **Client authentication**: `Off` (Public client)
6. **Authentication flow**: `Standard flow` (checked)
7. Click **"Next"**

8. **Valid redirect URIs**: 
   - Add: `http://localhost:8080/*`
   - Add: `http://guacamole:8080/*` (if using Docker network)
9. **Web origins**: `*` (or specific origins)
10. Click **"Save"**

### Step 3: Create Roles

1. Go to **Realm roles** (or **Clients** → `guacamole-client` → **Roles** for client roles)
2. Click **"Create role"**
3. Create role: `admin`
4. Click **"Save"**
5. Repeat to create: `console_accesser`

### Step 4: Create a Test User

1. Go to **Users** → **Create new user**
2. **Username**: `testuser` (or your preferred username)
3. **Email**: `testuser@example.com`
4. **Email verified**: `ON`
5. Click **"Create"**

6. Go to **Credentials** tab
7. Set a password (temporary password)
8. **Temporary**: `OFF` (so user doesn't need to change password)
9. Click **"Set password"**

### Step 5: Assign Roles to User

1. Go to **Users** → Select your user
2. Go to **Role Mappings** tab
3. Click **"Assign role"**
4. Select `admin` or `console_accesser` role
5. Click **"Assign"**

### Step 6: Configure Client Mapper for Roles

1. Go to **Clients** → `guacamole-client`
2. Go to **Client scopes** tab
3. Click on `guacamole-client-dedicated` (or create a new client scope)
4. Go to **Mappers** tab
5. Click **"Add mapper"** → **"By configuration"**
6. Select **"Realm roles"** or **"User Realm Role Mapping"**

7. Configure the mapper:
   - **Name**: `realm-roles`
   - **Token Claim Name**: `groups` (must match `openid-groups-claim-type` in guacamole.properties)
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
   - **Multivalued**: `ON`
   - **Claim JSON Type**: `String`
8. Click **"Save"**

### Step 7: Get Keycloak URLs

After setup, note these URLs for `guacamole.properties`:

- **Authorization Endpoint**: 
  ```
  http://localhost:8090/realms/myrealm/protocol/openid-connect/auth
  ```

- **JWKS Endpoint**: 
  ```
  http://localhost:8090/realms/myrealm/protocol/openid-connect/certs
  ```

- **Issuer**: 
  ```
  http://localhost:8090/realms/myrealm
  ```

## Update guacamole.properties

Update `deployment/guacamole.properties` with your Keycloak settings:

```properties
# OpenID Configuration
openid-authorization-endpoint: http://localhost:8090/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: http://localhost:8090/realms/myrealm/protocol/openid-connect/certs
openid-issuer: http://localhost:8090/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://localhost:8080/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile

# Guacd Configuration
guacd-hostname: guacd
guacd-port: 4822
```

## Testing the Setup

### 1. Start Keycloak

```bash
docker compose -f docker-compose-keycloak.yml up -d
```

### 2. Start Guacamole

```bash
docker compose up -d
```

### 3. Test Login

1. Open http://localhost:8080
2. You should be redirected to Keycloak login
3. Log in with your test user credentials
4. You should be redirected back to Guacamole
5. If user has `admin` or `console_accesser` role, connections from `user-mapping.xml` should be visible

## Using Docker Network (Optional)

If you want Guacamole and Keycloak to communicate via Docker network:

### Option 1: Use External Network

```bash
# Create shared network
docker network create guacamole-keycloak-network

# Start Keycloak with network
docker compose -f docker-compose-keycloak.yml up -d

# Connect Guacamole to network
docker network connect guacamole-keycloak-network guacamole-test
```

Then update `guacamole.properties` to use `keycloak:8090` instead of `localhost:8090`.

### Option 2: Update docker-compose.yml

Add Keycloak service to main docker-compose.yml or use external network reference.

## Verify Keycloak is Working

```bash
# Check Keycloak is running
docker compose -f docker-compose-keycloak.yml ps

# Check Keycloak logs
docker compose -f docker-compose-keycloak.yml logs -f keycloak

# Test Keycloak health
curl http://localhost:8090/health/ready
```

## Keycloak URLs Summary

- **Admin Console**: http://localhost:8090
- **Realm**: `myrealm` (or your realm name)
- **Client**: `guacamole-client`
- **Roles**: `admin`, `console_accesser`

## Troubleshooting

### Keycloak Not Starting

```bash
# Check logs
docker compose -f docker-compose-keycloak.yml logs keycloak

# Check database
docker compose -f docker-compose-keycloak.yml logs postgres
```

### Roles Not in Token

1. Verify mapper is configured correctly
2. Check "Token Claim Name" matches `openid-groups-claim-type` in guacamole.properties
3. Verify "Add to ID token" is ON
4. Test token at https://jwt.io (decode your JWT token)

### Connection Issues

If Guacamole can't reach Keycloak:
- Check Keycloak is running: `docker compose -f docker-compose-keycloak.yml ps`
- Check ports are accessible: `curl http://localhost:8090/health/ready`
- If using Docker network, ensure containers are on same network

## Stop Keycloak

```bash
docker compose -f docker-compose-keycloak.yml down

# Remove volumes (deletes all data)
docker compose -f docker-compose-keycloak.yml down -v
```

## Production Considerations

For production, you should:

1. **Change admin credentials** - Set strong passwords
2. **Use HTTPS** - Configure SSL/TLS certificates
3. **Use production database** - Not the embedded H2
4. **Set proper hostname** - Update `KC_HOSTNAME`
5. **Enable authentication** - Use proper authentication methods
6. **Backup database** - Regularly backup Keycloak data
7. **Use environment variables** - For sensitive configuration
8. **Configure reverse proxy** - Use nginx/traefik for SSL termination

