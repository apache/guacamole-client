# OpenBao Vault Extension for Apache Guacamole

This extension integrates Apache Guacamole with [OpenBao](https://openbao.org/) vault to automatically retrieve connection credentials from OpenBao at connection time.

## Overview

The OpenBao vault extension allows Guacamole to retrieve credentials from an OpenBao server using token-based authentication. Connection parameters configured with special tokens like `${OPENBAO_SECRET}` are automatically replaced with values retrieved from OpenBao when a user connects.

## Features

- **Automatic Credential Retrieval**: Fetches credentials from OpenBao without requiring users to re-enter passwords
- **Token-Based Resolution**: Uses `${OPENBAO_SECRET}` and `${GUAC_USERNAME}` tokens in connection parameters
- **KV v2 Support**: Works with OpenBao KV v2 secrets engine
- **Simple Configuration**: Minimal configuration required in `guacamole.properties`
- **Secure**: Uses OpenBao's token-based authentication for secure API access

## How It Works

1. User logs into Guacamole with their username
2. User initiates a connection configured with `${OPENBAO_SECRET}` token
3. Extension queries OpenBao API to retrieve the secret for that username
4. Password is extracted and injected into the connection parameters
5. Connection proceeds with the retrieved credentials

## Configuration

### OpenBao Server Setup

Before using this extension, you need:

1. An OpenBao server running and accessible from the Guacamole server
2. A KV v2 secrets engine mounted (eg path: `guacamole-credentails`)
3. An OpenBao authentication token with read access to the secrets
4. Secrets stored with a `password` field in the data

Example secret structure:
```json
{
  "data": {
    "data": {
      "username": "user1",
      "password": "SecretPassword123"
    }
  }
}
```

### Guacamole Configuration

Add the following properties to `guacamole.properties`:

```properties
# OpenBao server URL (required)
openbao-server-url: http://openbao.example.com:8200

# OpenBao authentication token (required)
openbao-token: s.YourTokenHere

# KV mount path (optional, default: guacamole-credentails)
openbao-mount-path: guacamole-credentails
```

**Note**: The extension uses hardcoded defaults for:
- KV version: `2` (KV v2 secrets engine)
- Connection timeout: `5000ms` (5 seconds)
- Request timeout: `10000ms` (10 seconds)

### Connection Configuration

When creating connections in Guacamole, use these token patterns:

- **`${OPENBAO_SECRET}`**: Replaced with the password from OpenBao
- **`${GUAC_USERNAME}`**: Replaced with the logged-in Guacamole username

Example RDP connection:
- Username: `${GUAC_USERNAME}`
- Password: `${OPENBAO_SECRET}`
- Hostname: `192.168.1.100`

## Secret Path Mapping

The extension maps Guacamole usernames directly to OpenBao secret paths:

```
Guacamole username: "john"
OpenBao secret path: /v1/guacamole-credentails/data/john
```

For each user, create a corresponding secret in OpenBao at the path matching their Guacamole username.

## Building

Build the extension from the guacamole-client source tree:

```bash
cd extensions/guacamole-vault
mvn clean package
```

The built extension will be located at:
```
modules/guacamole-vault-openbao/target/guacamole-vault-openbao-<version>.jar
```

## Installation

1. Copy the built JAR to the Guacamole extensions directory:
   ```bash
   cp guacamole-vault-openbao-*.jar /etc/guacamole/extensions/
   ```

2. Ensure `guacamole-vault-base-*.jar` is also present in the extensions directory (it's a dependency)

3. Configure `guacamole.properties` as described above

4. Restart Guacamole (e.g., restart Tomcat)

## Security Considerations

1. **Protect the OpenBao Token**: Use a dedicated token with minimal permissions (read-only access to required secret paths)

2. **Use TLS in Production**: Always use HTTPS URLs for OpenBao in production:
   ```properties
   openbao-server-url: https://openbao.example.com:8200
   ```

3. **Network Security**: Restrict OpenBao access to the Guacamole server using firewall rules

4. **Audit Logging**: Enable OpenBao audit logging to track credential access

5. **Token Rotation**: Regularly rotate OpenBao tokens and update the configuration

## Troubleshooting

### Extension Not Loading

Check the Guacamole logs (typically in Tomcat's `catalina.out`) for errors. Common issues:

- Missing `guacamole-vault-base` dependency
- Incorrect permissions on JAR files
- Configuration errors in `guacamole.properties`

### Secret Not Found

Error: `Secret not found in OpenBao for username: john`

Solutions:
1. Verify the secret exists in OpenBao at the expected path
2. Check that the Guacamole username matches the secret name in OpenBao
3. Verify the token has read access to the secret

### Permission Denied

Error: `Permission denied accessing OpenBao. Check token permissions.`

Solutions:
1. Verify the token has appropriate policies attached
2. Check that the token hasn't expired
3. Ensure the token has read access to the KV mount path

### Connection Timeout

Error: `Failed to communicate with OpenBao`

Solutions:
1. Verify OpenBao is accessible from the Guacamole server
2. Check firewall rules between Guacamole and OpenBao
3. Verify the OpenBao URL is correct in the configuration

## Example Deployment

1. **Setup OpenBao**:
   ```bash
   # Start OpenBao
   bao server -dev

   # Enable KV v2 engine
   bao secrets enable -path=guacamole-credentails kv-v2

   # Create a secret
   bao kv put guacamole-credentails/john password=SecretPass123
   ```

2. **Configure Guacamole**:
   ```properties
   openbao-server-url: http://openbao.example.com:8200
   openbao-token: s.yourtokenhere
   openbao-mount-path: guacamole-credentails
   ```

3. **Create Connection**:
   - Name: Windows Server
   - Protocol: RDP
   - Hostname: 192.168.1.100
   - Username: `${GUAC_USERNAME}`
   - Password: `${OPENBAO_SECRET}`

4. **Connect**: Log in as user "john" and connect to the Windows Server connection. The password will be automatically retrieved from OpenBao.

## License

This extension is licensed under the Apache License, Version 2.0. See the LICENSE file for details.

## Support

For issues or questions:
- Apache Guacamole: https://guacamole.apache.org/
- OpenBao: https://openbao.org/
- Issue Tracker: https://issues.apache.org/jira/browse/GUACAMOLE/
