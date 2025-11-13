# Keycloak Roles Setup for OpenID Extension

This document explains how to configure Keycloak to provide roles in the JWT token so that the OpenID extension can check for the `admin` or `console_accesser` roles.

## Overview

The OpenID extension checks for Keycloak roles (`admin` or `console_accesser`) before providing connections from `user-mapping.xml`. The roles must be included in the JWT token's groups claim.

## Keycloak Configuration

### 1. Create Roles in Keycloak

1. Log into Keycloak Admin Console
2. Select your realm
3. Go to **Realm roles** or **Client roles** (depending on where you want to store the roles)
4. Create the following roles:
   - `admin`
   - `console_accesser`

### 2. Assign Roles to Users

1. Go to **Users** → Select a user
2. Go to **Role Mappings** tab
3. Assign the `admin` or `console_accesser` role to the user

### 3. Configure Client to Include Roles in Token

1. Go to **Clients** → Select your Guacamole client
2. Go to **Mappers** tab
3. Create a new mapper or use an existing one:

#### Option A: Use Built-in "realm roles" Mapper (for Realm Roles)

1. Click **Add Builtin** or **Add Mapper**
2. Select **realm roles** mapper
3. Configure:
   - **Name**: `realm-roles` (or any name)
   - **Token Claim Name**: `groups` (or match your `openid-groups-claim-type` setting)
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
   - **Multivalued**: `ON`
   - **Claim JSON Type**: `String`

#### Option B: Create Custom Mapper (for Client Roles)

1. Click **Add Mapper** → **By configuration**
2. Select **User Realm Role Mapping** or **User Client Role Mapping**
3. Configure:
   - **Name**: `client-roles` (or any name)
   - **Client ID**: Your client ID (if using client roles)
   - **Token Claim Name**: `groups` (or match your `openid-groups-claim-type` setting)
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
   - **Multivalued**: `ON`
   - **Claim JSON Type**: `String`

### 4. Configure Guacamole Properties

In your `guacamole.properties`, ensure the groups claim type matches what you configured in Keycloak:

```properties
# Groups claim type - should match the Token Claim Name in Keycloak mapper
openid-groups-claim-type: groups
```

If you're using a different claim name (like `roles` or `realm_access.roles`), update accordingly:

```properties
# Example: Using 'roles' claim
openid-groups-claim-type: roles

# Example: Using nested realm_access.roles (requires custom mapper)
openid-groups-claim-type: realm_access.roles
```

## JWT Token Structure

After configuration, your JWT token should include roles in the groups claim. Example:

```json
{
  "sub": "user-id",
  "email": "user@example.com",
  "groups": ["admin", "console_accesser", "other-role"],
  ...
}
```

Or if using realm_access:

```json
{
  "sub": "user-id",
  "email": "user@example.com",
  "realm_access": {
    "roles": ["admin", "console_accesser"]
  },
  ...
}
```

## Testing

1. **Check JWT Token**: Decode your JWT token (using jwt.io or similar) and verify that roles are present in the expected claim.

2. **Check Logs**: When a user logs in, check Guacamole logs for messages like:
   ```
   User username has required role (admin: true, console_accesser: false)
   ```
   or
   ```
   User username does not have required role (admin or console_accesser). No connections from user-mapping.xml will be provided.
   ```

3. **Verify Connections**: Users with `admin` or `console_accesser` roles should see connections from `user-mapping.xml` in the ROOT group. Users without these roles should not see any connections.

## Troubleshooting

### Roles Not Appearing in Token

1. **Check Mapper Configuration**: Ensure the mapper is enabled and configured to add roles to ID token
2. **Check Role Assignment**: Verify the user actually has the role assigned
3. **Check Claim Name**: Ensure `openid-groups-claim-type` in `guacamole.properties` matches the Token Claim Name in Keycloak mapper
4. **Check Token Scope**: Ensure the token includes the necessary scopes (roles are typically included by default)

### Connections Not Showing

1. **Check Logs**: Look for role check messages in Guacamole logs
2. **Verify Role Names**: Ensure role names match exactly: `admin` or `console_accesser` (case-sensitive)
3. **Check user-mapping.xml**: Verify the file exists and contains valid connections
4. **Check File Location**: Ensure `user-mapping.xml` is in `GUACAMOLE_HOME` directory

### Common Issues

- **Case Sensitivity**: Role names are case-sensitive. `Admin` is different from `admin`
- **Claim Name Mismatch**: The claim name in Keycloak must match `openid-groups-claim-type` in guacamole.properties
- **Token Not Refreshed**: After changing roles, users may need to log out and log back in to get a new token

## Advanced: Custom Role Claim Path

If your Keycloak setup uses a nested structure for roles (like `realm_access.roles`), you may need to create a custom mapper or adjust the claim extraction logic. The default implementation expects a simple array of strings in the groups claim.

For nested structures, you might need to:
1. Use a custom mapper in Keycloak to flatten the structure
2. Or modify the `TokenValidationService.processGroups()` method to handle nested claims

## Example Keycloak Mapper Configuration

**Realm Roles Mapper:**
- **Mapper Type**: User Realm Role Mapping
- **Name**: `realm-roles`
- **Token Claim Name**: `groups`
- **Add to ID token**: `ON`
- **Add to access token**: `ON`
- **Multivalued**: `ON`
- **Claim JSON Type**: `String`

This will add all realm roles to the `groups` claim in the JWT token.

