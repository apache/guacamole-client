# user-mapping.xml Example for OpenID Extension

This is an example `user-mapping.xml` file that demonstrates how to configure connections for use with the OpenID authentication extension.

## Important Notes

1. **ROLE-BASED ACCESS CONTROL** - Connections from this file are ONLY available to users who have the `admin` or `console_accesser` role from Keycloak. Users without these roles will NOT see any connections.

2. **Passwords in `<authorize>` tags are NOT used for authentication** - When using OpenID/Keycloak, authentication is handled by the identity provider. The username/password in the `<authorize>` tags are only required for XML structure validity.

3. **All connections are aggregated** - All connections from all users defined in this file will be made available to authorized users (those with `admin` or `console_accesser` role) and placed in the ROOT connection group.

4. **File location** - Place this file at `GUACAMOLE_HOME/user-mapping.xml`. Guacamole checks in this order:
   - System property: `guacamole.home`
   - Environment variable: `GUACAMOLE_HOME`
   - `~/.guacamole/user-mapping.xml`
   - `/etc/guacamole/user-mapping.xml`
   
   In Docker, typically mount to `/etc/guacamole/user-mapping.xml` and set `GUACAMOLE_HOME=/etc/guacamole`.

5. **Automatic reload** - The file is automatically re-read when modified. No restart of Guacamole is required.

## File Structure

### Basic Structure

```xml
<user-mapping>
    <authorize username="USERNAME" password="PASSWORD">
        <!-- Connections go here -->
    </authorize>
</user-mapping>
```

### Single Connection (Simplified Format)

When a user has only one connection, you can use the simplified format:

```xml
<authorize username="user" password="dummy">
    <protocol>vnc</protocol>
    <param name="hostname">server.example.com</param>
    <param name="port">5900</param>
    <param name="password">VNCPASS</param>
</authorize>
```

### Multiple Connections

When a user has multiple connections, use `<connection>` tags:

```xml
<authorize username="user" password="dummy">
    <connection name="connection-1">
        <protocol>vnc</protocol>
        <param name="hostname">server1.example.com</param>
        <param name="port">5900</param>
    </connection>
    <connection name="connection-2">
        <protocol>rdp</protocol>
        <param name="hostname">server2.example.com</param>
        <param name="port">3389</param>
    </connection>
</authorize>
```

## Supported Protocols

### VNC (Virtual Network Computing)

```xml
<connection name="vnc-connection">
    <protocol>vnc</protocol>
    <param name="hostname">vnc-server.example.com</param>
    <param name="port">5900</param>
    <param name="password">VNCPASSWORD</param>
    <param name="color-depth">24</param>
    <param name="dpi">96</param>
    <param name="read-only">false</param>
</connection>
```

### RDP (Remote Desktop Protocol)

```xml
<connection name="rdp-connection">
    <protocol>rdp</protocol>
    <param name="hostname">rdp-server.example.com</param>
    <param name="port">3389</param>
    <param name="username">administrator</param>
    <param name="password">RDPPASSWORD</param>
    <param name="domain">DOMAIN</param>
    <param name="security">rdp</param>
    <param name="ignore-cert">true</param>
    <param name="enable-drive">true</param>
    <param name="drive-path">/tmp</param>
</connection>
```

### SSH (Secure Shell)

```xml
<connection name="ssh-connection">
    <protocol>ssh</protocol>
    <param name="hostname">ssh-server.example.com</param>
    <param name="port">22</param>
    <param name="username">${GUAC_USERNAME}</param>
    <param name="private-key">-----BEGIN RSA PRIVATE KEY-----
...key content...
-----END RSA PRIVATE KEY-----</param>
    <param name="font-name">DejaVu Sans Mono</param>
    <param name="font-size">12</param>
</connection>
```

## Token Substitution

You can use the following tokens in connection parameters:

- `${GUAC_USERNAME}` - The username of the authenticated user (from OpenID)
- `${GUAC_PASSWORD}` - Not typically used with OpenID
- Other tokens as defined by your Guacamole configuration

## Password Encoding

While passwords in `<authorize>` tags aren't used for OpenID authentication, you can still specify encoding for consistency:

```xml
<!-- Plain text -->
<authorize username="user" password="plaintext-password">

<!-- MD5 hash -->
<authorize username="user" 
          password="319f4d26e3c536b5dd871bb2c52e3178" 
          encoding="md5">

<!-- SHA-256 hash -->
<authorize username="user" 
          password="sha256-hash-here" 
          encoding="sha-256">
```

## Best Practices

1. **Use descriptive connection names** - Connection names should clearly identify the server/purpose.

2. **Group related connections** - Organize connections by user/role even though all connections are available to everyone.

3. **Use tokens when possible** - Use `${GUAC_USERNAME}` for SSH usernames to map to the OpenID username.

4. **Keep private keys secure** - Store SSH private keys securely and consider using environment variables or external key management.

5. **Test after changes** - After modifying the file, verify connections appear in the Guacamole interface.

6. **Document your connections** - Add comments in the XML to document what each connection is for.

## Example Use Case

If your service discovery updates `user-mapping.xml` with discovered services, the structure might look like:

```xml
<user-mapping>
    <!-- Auto-discovered services from service discovery -->
    <authorize username="service-discovery" password="dummy">
        <connection name="service-1">
            <protocol>vnc</protocol>
            <param name="hostname">service-1.internal</param>
            <param name="port">5900</param>
        </connection>
        <connection name="service-2">
            <protocol>rdp</protocol>
            <param name="hostname">service-2.internal</param>
            <param name="port">3389</param>
        </connection>
    </authorize>
</user-mapping>
```

All OpenID-authenticated users will see both `service-1` and `service-2` in their ROOT connection group.

