# Docker Deployment Guide for OpenID Extension with user-mapping.xml

This guide explains how to package and deploy the OpenID extension with user-mapping.xml support in a Guacamole Docker container.

## File Locations

### user-mapping.xml Location

The `user-mapping.xml` file is read from `GUACAMOLE_HOME/user-mapping.xml`. Guacamole determines `GUACAMOLE_HOME` in the following order:

1. **System Property**: `guacamole.home` (set via `-Dguacamole.home=/path/to/home`)
2. **Environment Variable**: `GUACAMOLE_HOME` (set in Docker)
3. **Default Locations**:
   - `~/.guacamole/user-mapping.xml` (user's home directory)
   - `/etc/guacamole/user-mapping.xml` (system-wide)

### Extension JAR Location

The OpenID extension JAR must be placed in:
- `GUACAMOLE_HOME/extensions/guacamole-auth-sso-openid-*.jar`

## Building the Extension

### 1. Build the Extension JAR

```bash
cd /root/guacamole-client/extensions/guacamole-auth-sso
mvn clean package
```

The JAR file will be created at:
```
extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/target/guacamole-auth-sso-openid-*.jar
```

### 2. Copy Required Files

```bash
# Create deployment directory
mkdir -p /path/to/deployment

# Copy the extension JAR
cp extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/target/guacamole-auth-sso-openid-*.jar \
   /path/to/deployment/extensions/

# Copy user-mapping.xml
cp user-mapping-example.xml /path/to/deployment/user-mapping.xml
```

## Docker Deployment Options

### Option 1: Using Volume Mounts (Recommended)

Create a directory structure on your host:

```
/opt/guacamole/
├── extensions/
│   └── guacamole-auth-sso-openid-*.jar
├── user-mapping.xml
└── guacamole.properties
```

**docker-compose.yml:**

```yaml
version: '3.8'

services:
  guacamole:
    image: guacamole/guacamole:latest
    container_name: guacamole
    environment:
      - GUACD_HOSTNAME=guacd
      - GUACD_PORT=4822
      # Set GUACAMOLE_HOME to /etc/guacamole
      - GUACAMOLE_HOME=/etc/guacamole
    volumes:
      # Mount the entire configuration directory
      - /opt/guacamole:/etc/guacamole:ro
    depends_on:
      - guacd
    ports:
      - "8080:8080"

  guacd:
    image: guacamole/guacd:latest
    container_name: guacd
    ports:
      - "4822:4822"
```

**Run:**

```bash
docker compose up -d
```

### Option 2: Using Dockerfile (Custom Image)

Create a `Dockerfile`:

```dockerfile
FROM guacamole/guacamole:latest

# Set GUACAMOLE_HOME
ENV GUACAMOLE_HOME=/etc/guacamole

# Copy extension JAR
COPY extensions/guacamole-auth-sso-openid-*.jar /etc/guacamole/extensions/

# Copy user-mapping.xml
COPY user-mapping.xml /etc/guacamole/user-mapping.xml

# Copy guacamole.properties (if needed)
COPY guacamole.properties /etc/guacamole/guacamole.properties
```

**Build and run:**

```bash
# Build custom image
docker build -t my-guacamole:latest .

# Run container
docker run -d \
  --name guacamole \
  -p 8080:8080 \
  -e GUACD_HOSTNAME=guacd \
  -e GUACD_PORT=4822 \
  --link guacd:guacd \
  my-guacamole:latest
```

### Option 3: Using Init Container or Entrypoint Script

If you need to dynamically update `user-mapping.xml`, you can use an init container or entrypoint script:

**docker-compose.yml with init container:**

```yaml
version: '3.8'

services:
  guacamole-init:
    image: busybox
    volumes:
      - guacamole-data:/etc/guacamole
    command: sh -c "
      mkdir -p /etc/guacamole/extensions &&
      cp /source/extensions/*.jar /etc/guacamole/extensions/ &&
      cp /source/user-mapping.xml /etc/guacamole/user-mapping.xml &&
      chmod -R 755 /etc/guacamole
    "
    volumes:
      - ./deployment:/source:ro

  guacamole:
    image: guacamole/guacamole:latest
    environment:
      - GUACAMOLE_HOME=/etc/guacamole
    volumes:
      - guacamole-data:/etc/guacamole:ro
    depends_on:
      - guacamole-init
    ports:
      - "8080:8080"

volumes:
  guacamole-data:
```

## Configuration Files

### guacamole.properties

Example `guacamole.properties` for OpenID with Keycloak:

```properties
# OpenID Configuration
openid-authorization-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/auth
openid-jwks-endpoint: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs
openid-issuer: https://keycloak.example.com/auth/realms/myrealm
openid-client-id: guacamole-client
openid-redirect-uri: http://guacamole.example.com/
openid-username-claim-type: email
openid-groups-claim-type: groups
openid-scope: openid email profile

# Guacd Configuration
guacd-hostname: guacd
guacd-port: 4822
```

### user-mapping.xml

Place your `user-mapping.xml` file in the same directory as `guacamole.properties`:

```
/etc/guacamole/
├── extensions/
│   └── guacamole-auth-sso-openid-*.jar
├── user-mapping.xml
└── guacamole.properties
```

## Verifying Deployment

### 1. Check Extension is Loaded

Check Guacamole logs for:

```
Extension "OpenID Authentication Extension" loaded.
```

### 2. Check user-mapping.xml is Read

Check logs for:

```
Reading user mapping file: "/etc/guacamole/user-mapping.xml"
```

### 3. Verify Role-Based Access

- **User with `admin` or `console_accesser` role**: Should see connections from `user-mapping.xml`
- **User without required roles**: Should NOT see any connections

Check logs for role verification:

```
User username has required role (admin: true, console_accesser: false)
```

or

```
User username does not have required role (admin or console_accesser). No connections from user-mapping.xml will be provided.
```

## Updating user-mapping.xml

The file is automatically re-read when modified. You can update it without restarting:

1. **With Volume Mount**: Edit the file on the host, changes are immediately available
2. **In Container**: Copy updated file into container:
   ```bash
   docker cp user-mapping.xml guacamole:/etc/guacamole/user-mapping.xml
   ```

No restart required - Guacamole will detect the change and reload.

## Troubleshooting

### Extension Not Loading

1. **Check JAR location**: Must be in `GUACAMOLE_HOME/extensions/`
2. **Check file permissions**: JAR must be readable
3. **Check logs**: Look for extension loading errors

### user-mapping.xml Not Found

1. **Check file location**: Must be at `GUACAMOLE_HOME/user-mapping.xml`
2. **Check GUACAMOLE_HOME**: Verify environment variable is set correctly
3. **Check logs**: Look for "User mapping file does not exist" messages

### Connections Not Showing

1. **Check user roles**: User must have `admin` or `console_accesser` role in Keycloak
2. **Check Keycloak mapper**: Ensure roles are included in JWT token's `groups` claim
3. **Check guacamole.properties**: Verify `openid-groups-claim-type` matches Keycloak configuration
4. **Check logs**: Look for role verification messages

### File Not Auto-Reloading

1. **Check file modification time**: Ensure the file timestamp changes when you edit it
2. **Check file permissions**: File must be readable
3. **Check logs**: Look for "Reading user mapping file" messages

## Complete Example Structure

```
/opt/guacamole/
├── extensions/
│   └── guacamole-auth-sso-openid-1.6.0.jar
├── user-mapping.xml
└── guacamole.properties
```

**docker-compose.yml:**

```yaml
version: '3.8'

services:
  guacamole:
    image: guacamole/guacamole:latest
    environment:
      - GUACAMOLE_HOME=/etc/guacamole
      - GUACD_HOSTNAME=guacd
      - GUACD_PORT=4822
    volumes:
      - ./guacamole:/etc/guacamole:ro
    ports:
      - "8080:8080"
    depends_on:
      - guacd

  guacd:
    image: guacamole/guacd:latest
    ports:
      - "4822:4822"
```

## Security Considerations

1. **File Permissions**: Ensure `user-mapping.xml` has appropriate permissions (read-only for Guacamole user)
2. **Volume Mounts**: Use read-only mounts (`:ro`) when possible
3. **Secrets**: Consider using environment variables or secrets management for sensitive connection parameters
4. **Role Validation**: Always verify users have required roles before granting access

## Production Recommendations

1. **Use ConfigMaps/Secrets**: In Kubernetes, use ConfigMaps for `user-mapping.xml` and Secrets for sensitive data
2. **Health Checks**: Add health check endpoints to verify extension is working
3. **Monitoring**: Monitor logs for role check failures and connection access patterns
4. **Backup**: Regularly backup `user-mapping.xml` and `guacamole.properties`

