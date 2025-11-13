# user-mapping.xml File Location Explanation

## Where the File is Read From

The `user-mapping.xml` file is read by the `UserMappingService` class in the OpenID extension.

### Code Location

**File**: `extensions/guacamole-auth-sso/modules/guacamole-auth-sso-openid/src/main/java/org/apache/guacamole/auth/openid/usermapping/UserMappingService.java`

**Relevant Code** (lines 85-86):
```java
// Read user mapping from GUACAMOLE_HOME/user-mapping.xml
File userMappingFile = new File(environment.getGuacamoleHome(), USER_MAPPING_FILENAME);
```

Where:
- `environment.getGuacamoleHome()` returns the `GUACAMOLE_HOME` directory
- `USER_MAPPING_FILENAME` is the constant `"user-mapping.xml"`

### How GUACAMOLE_HOME is Determined

The `GUACAMOLE_HOME` directory is determined by Guacamole's `LocalEnvironment` class in this order:

1. **System Property**: `guacamole.home`
   - Set via: `-Dguacamole.home=/path/to/home`
   - Example: `java -Dguacamole.home=/opt/guacamole -jar guacamole.war`

2. **Environment Variable**: `GUACAMOLE_HOME`
   - Set via: `export GUACAMOLE_HOME=/path/to/home`
   - In Docker: `-e GUACAMOLE_HOME=/etc/guacamole`

3. **Default Locations** (checked in order):
   - `~/.guacamole/` (user's home directory)
   - `/etc/guacamole/` (if `/etc` exists)

### Full File Path Resolution

The complete file path is constructed as:
```
GUACAMOLE_HOME + "/" + "user-mapping.xml"
```

Examples:
- If `GUACAMOLE_HOME=/etc/guacamole` → `/etc/guacamole/user-mapping.xml`
- If `GUACAMOLE_HOME=/opt/guacamole` → `/opt/guacamole/user-mapping.xml`
- If using default `~/.guacamole` → `~/.guacamole/user-mapping.xml`

## Docker Container Considerations

### Standard Guacamole Docker Image

The official Guacamole Docker image uses a temporary `GUACAMOLE_HOME` by default. To use a persistent location:

1. **Set Environment Variable**:
   ```yaml
   environment:
     - GUACAMOLE_HOME=/etc/guacamole
   ```

2. **Mount Volume**:
   ```yaml
   volumes:
     - ./guacamole:/etc/guacamole:ro
   ```

3. **File Structure**:
   ```
   /etc/guacamole/
   ├── extensions/
   │   └── guacamole-auth-sso-openid-*.jar
   ├── user-mapping.xml          ← File is read from here
   └── guacamole.properties
   ```

### Verification

To verify where Guacamole is looking for the file, check the logs:

```
Reading user mapping file: "/etc/guacamole/user-mapping.xml"
```

Or if file not found:

```
User mapping file "/etc/guacamole/user-mapping.xml" does not exist and will not be read.
```

## File Reading Behavior

1. **Initial Read**: File is read when first accessed (lazy loading)
2. **Auto-Reload**: File is automatically re-read when modification time changes
3. **Caching**: File contents are cached until file is modified
4. **Error Handling**: If file doesn't exist or can't be read, no connections are provided (graceful degradation)

## Summary

- **File Name**: `user-mapping.xml` (fixed)
- **Directory**: `GUACAMOLE_HOME` (configurable)
- **Full Path**: `GUACAMOLE_HOME/user-mapping.xml`
- **In Docker**: Typically `/etc/guacamole/user-mapping.xml` with `GUACAMOLE_HOME=/etc/guacamole`

