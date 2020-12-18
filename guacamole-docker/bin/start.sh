#!/bin/sh -e
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

##
## @fn start.sh
##
## Automatically configures and starts Guacamole under Tomcat. Guacamole's
## guacamole.properties file will be automatically generated based on the
## linked database container (either MySQL or PostgreSQL) and the linked guacd
## container. The Tomcat process will ultimately replace the process of this
## script, running in the foreground until terminated.
##

GUACAMOLE_HOME_TEMPLATE="$GUACAMOLE_HOME"

GUACAMOLE_HOME="$HOME/.guacamole"
GUACAMOLE_EXT="$GUACAMOLE_HOME/extensions"
GUACAMOLE_LIB="$GUACAMOLE_HOME/lib"
GUACAMOLE_PROPERTIES="$GUACAMOLE_HOME/guacamole.properties"

##
## Sets the given property to the given value within guacamole.properties,
## creating guacamole.properties first if necessary.
##
## @param NAME
##     The name of the property to set.
##
## @param VALUE
##     The value to set the property to.
##
set_property() {

    NAME="$1"
    VALUE="$2"

    # Ensure guacamole.properties exists
    if [ ! -e "$GUACAMOLE_PROPERTIES" ]; then
        mkdir -p "$GUACAMOLE_HOME"
        echo "# guacamole.properties - generated `date`" > "$GUACAMOLE_PROPERTIES"
    fi

    # Set property
    echo "$NAME: $VALUE" >> "$GUACAMOLE_PROPERTIES"

}

##
## Sets the given property to the given value within guacamole.properties only
## if a value is provided, creating guacamole.properties first if necessary.
##
## @param NAME
##     The name of the property to set.
##
## @param VALUE
##     The value to set the property to, if any. If omitted or empty, the
##     property will not be set.
##
set_optional_property() {

    NAME="$1"
    VALUE="$2"

    # Set the property only if a value is provided
    if [ -n "$VALUE" ]; then
        set_property "$NAME" "$VALUE"
    fi

}

# Print error message regarding missing required variables for MySQL authentication
mysql_missing_vars() {
   cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using a MySQL database, you must provide each of the following
environment variables or their corresponding Docker secrets by appending _FILE
to the environment variable, and setting the value to the path of the 
corresponding secret:

    MYSQL_USER         The user to authenticate as when connecting to
                       MySQL.

    MYSQL_PASSWORD     The password to use when authenticating with MySQL as
                       MYSQL_USER.

    MYSQL_DATABASE     The name of the MySQL database to use for Guacamole
                       authentication.
END
    exit 1;
}


##
## Adds properties to guacamole.properties which select the MySQL
## authentication provider, and configure it to connect to the linked MySQL
## container. If a MySQL database is explicitly specified using the
## MYSQL_HOSTNAME and MYSQL_PORT environment variables, that will be used
## instead of a linked container.
##
associate_mysql() {

    # Use linked container if specified
    if [ -n "$MYSQL_NAME" ]; then
        MYSQL_HOSTNAME="$MYSQL_PORT_3306_TCP_ADDR"
        MYSQL_PORT="$MYSQL_PORT_3306_TCP_PORT"
    fi

    # Use default port if none specified
    MYSQL_PORT="${MYSQL_PORT-3306}"

    # Verify required connection information is present
    if [ -z "$MYSQL_HOSTNAME" -o -z "$MYSQL_PORT" ]; then
        cat <<END
FATAL: Missing MYSQL_HOSTNAME or "mysql" link.
-------------------------------------------------------------------------------
If using a MySQL database, you must either:

(a) Explicitly link that container with the link named "mysql".

(b) If not using a Docker container for MySQL, explicitly specify the TCP
    connection to your database using the following environment variables:

    MYSQL_HOSTNAME     The hostname or IP address of the MySQL server. If not
                       using a MySQL Docker container and corresponding link,
                       this environment variable is *REQUIRED*.

    MYSQL_PORT         The port on which the MySQL server is listening for TCP
                       connections. This environment variable is option. If
                       omitted, the standard MySQL port of 3306 will be used.
END
        exit 1;
    fi


    # Verify that the required Docker secrets are present, else, default to their normal environment variables
    if [ -n "$MYSQL_USER_FILE" ]; then
        set_property "mysql-username" "`cat "$MYSQL_USER_FILE"`"
    elif [ -n "$MYSQL_USER" ]; then
        set_property "mysql-username" "$MYSQL_USER"
    else
        mysql_missing_vars
        exit 1;
    fi
    
    if [ -n "$MYSQL_PASSWORD_FILE" ]; then
        set_property "mysql-password" "`cat "$MYSQL_PASSWORD_FILE"`"
    elif [ -n "$MYSQL_PASSWORD" ]; then
        set_property "mysql-password" "$MYSQL_PASSWORD"
    else
        mysql_missing_vars
        exit 1;
    fi

    if [ -n "$MYSQL_DATABASE_FILE" ]; then
        set_property "mysql-database" "`cat "$MYSQL_DATABASE_FILE"`"
    elif [ -n "$MYSQL_DATABASE" ]; then
        set_property "mysql-database" "$MYSQL_DATABASE"
    else
        mysql_missing_vars
        exit 1;
    fi

    # Update config file
    set_property "mysql-hostname" "$MYSQL_HOSTNAME"
    set_property "mysql-port"     "$MYSQL_PORT"

    set_optional_property               \
        "mysql-absolute-max-connections" \
        "$MYSQL_ABSOLUTE_MAX_CONNECTIONS"

    set_optional_property               \
        "mysql-default-max-connections" \
        "$MYSQL_DEFAULT_MAX_CONNECTIONS"

    set_optional_property                     \
        "mysql-default-max-group-connections" \
        "$MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS"

    set_optional_property                        \
        "mysql-default-max-connections-per-user" \
        "$MYSQL_DEFAULT_MAX_CONNECTIONS_PER_USER"

    set_optional_property                              \
        "mysql-default-max-group-connections-per-user" \
        "$MYSQL_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER"

    set_optional_property     \
        "mysql-user-required" \
        "$MYSQL_USER_REQUIRED"

    set_optional_property \
        "mysql-ssl-mode"  \
        "$MYSQL_SSL_MODE"

    set_optional_property        \
        "mysql-ssl-trust-store"  \
        "$MYSQL_SSL_TRUST_STORE"

    # For SSL trust store password, check secrets, first, then standard env variable
    if [ -n "$MYSQL_SSL_TRUST_PASSWORD_FILE" ]; then
        set_property "mysql-ssl-trust-password" "`cat "$MYSQL_SSL_TRUST_PASSWORD_FILE"`"
    elif [ -n "$MYSQL_SSL_TRUST_PASSWORD" ]; then
        set_property "mysql-ssl-trust-password" "$MYSQL_SSL_TRUST_PASSWORD"
    fi

    set_optional_property         \
        "mysql-ssl-client-store"  \
        "$MYSQL_SSL_CLIENT_STORE"

    # For SSL trust store password, check secrets, first, then standard env variable
    if [ -n "$MYSQL_SSL_CLIENT_PASSWORD_FILE" ]; then
        set_property "mysql-ssl-client-password" "`cat "$MYSQL_SSL_CLIENT_PASSWORD_FILE"`"
    elif [ -n "$MYSQL_SSL_CLIENT_PASSWORD" ]; then
        set_property "mysql-ssl-client-password" "$MYSQL_SSL_CLIENT_PASSWORD"
    fi

    # Add required .jar files to GUACAMOLE_LIB and GUACAMOLE_EXT
    ln -s /opt/guacamole/mysql/mysql-connector-*.jar "$GUACAMOLE_LIB"
    ln -s /opt/guacamole/mysql/guacamole-auth-*.jar "$GUACAMOLE_EXT"

}

# Print error message regarding missing required variables for PostgreSQL authentication
postgres_missing_vars() {
    cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using a PostgreSQL database, you must provide each of the following
environment variables or their corresponding Docker secrets by appending _FILE
to the environment variable, and setting the value to the path of the 
corresponding secret:

    POSTGRES_USER      The user to authenticate as when connecting to
                       PostgreSQL.

    POSTGRES_PASSWORD  The password to use when authenticating with PostgreSQL
                       as POSTGRES_USER.

    POSTGRES_DATABASE  The name of the PostgreSQL database to use for Guacamole
                       authentication.
END
    exit 1;
}

##
## Adds properties to guacamole.properties which select the PostgreSQL
## authentication provider, and configure it to connect to the linked
## PostgreSQL container. If a PostgreSQL database is explicitly specified using
## the POSTGRES_HOSTNAME and POSTGRES_PORT environment variables, that will be
## used instead of a linked container.
##
associate_postgresql() {

    # Use linked container if specified
    if [ -n "$POSTGRES_NAME" ]; then
        POSTGRES_HOSTNAME="$POSTGRES_PORT_5432_TCP_ADDR"
        POSTGRES_PORT="$POSTGRES_PORT_5432_TCP_PORT"
    fi

    # Use default port if none specified
    POSTGRES_PORT="${POSTGRES_PORT-5432}"

    # Verify required connection information is present
    if [ -z "$POSTGRES_HOSTNAME" -o -z "$POSTGRES_PORT" ]; then
        cat <<END
FATAL: Missing POSTGRES_HOSTNAME or "postgres" link.
-------------------------------------------------------------------------------
If using a PostgreSQL database, you must either:

(a) Explicitly link that container with the link named "postgres".

(b) If not using a Docker container for PostgreSQL, explicitly specify the TCP
    connection to your database using the following environment variables:

    POSTGRES_HOSTNAME  The hostname or IP address of the PostgreSQL server. If
                       not using a PostgreSQL Docker container and
                       corresponding link, this environment variable is
                       *REQUIRED*.

    POSTGRES_PORT      The port on which the PostgreSQL server is listening for
                       TCP connections. This environment variable is option. If
                       omitted, the standard PostgreSQL port of 5432 will be
                       used.
END
        exit 1;
    fi

    # Verify that the required Docker secrets are present, else, default to their normal environment variables
    if [ -n "$POSTGRES_USER_FILE" ]; then
        set_property "postgresql-username" "`cat "$POSTGRES_USER_FILE"`"
    elif [ -n "$POSTGRES_USER" ]; then
        set_property "postgresql-username" "$POSTGRES_USER"
    else
        postgres_missing_vars
        exit 1;
    fi
    
    if [ -n "$POSTGRES_PASSWORD_FILE" ]; then
        set_property "postgresql-password" "`cat "$POSTGRES_PASSWORD_FILE"`"
    elif [ -n "$POSTGRES_PASSWORD" ]; then
        set_property "postgresql-password" "$POSTGRES_PASSWORD"
    else
        postgres_missing_vars
        exit 1;
    fi

    if [ -n "$POSTGRES_DATABASE_FILE" ]; then
        set_property "postgresql-database" "`cat "$POSTGRES_DATABASE_FILE"`"
    elif [ -n "$POSTGRES_DATABASE" ]; then
        set_property "postgresql-database" "$POSTGRES_DATABASE"
    else
        postgres_missing_vars
        exit 1;
    fi

    # Update config file
    set_property "postgresql-hostname" "$POSTGRES_HOSTNAME"
    set_property "postgresql-port"     "$POSTGRES_PORT"

    set_optional_property               \
        "postgresql-absolute-max-connections" \
        "$POSTGRES_ABSOLUTE_MAX_CONNECTIONS"

    set_optional_property                    \
        "postgresql-default-max-connections" \
        "$POSTGRES_DEFAULT_MAX_CONNECTIONS"

    set_optional_property                          \
        "postgresql-default-max-group-connections" \
        "$POSTGRES_DEFAULT_MAX_GROUP_CONNECTIONS"

    set_optional_property                             \
        "postgresql-default-max-connections-per-user" \
        "$POSTGRES_DEFAULT_MAX_CONNECTIONS_PER_USER"

    set_optional_property                                   \
        "postgresql-default-max-group-connections-per-user" \
        "$POSTGRES_DEFAULT_MAX_GROUP_CONNECTIONS_PER_USER"

    set_optional_property                      \
        "postgresql-default-statement-timeout" \
        "$POSTGRES_DEFAULT_STATEMENT_TIMEOUT"

    set_optional_property          \
        "postgresql-user-required" \
        "$POSTGRES_USER_REQUIRED"

    set_optional_property           \
        "postgresql-socket-timeout" \
        "$POSTGRES_SOCKET_TIMEOUT"

    set_optional_property      \
        "postgresql-ssl-mode"  \
        "$POSTGRESQL_SSL_MODE"

    set_optional_property           \
        "postgresql-ssl-cert-file"  \
        "$POSTGRESQL_SSL_CERT_FILE"

    set_optional_property          \
        "postgresql-ssl-key-file"  \
        "$POSTGRESQL_SSL_KEY_FILE"

    set_optional_property                \
        "postgresql-ssl-root-cert-file"  \
        "$POSTGRESQL_SSL_ROOT_CERT_FILE"

    # For SSL key password, check secrets, first, then standard env variable
    if [ -n "$POSTGRES_SSL_KEY_PASSWORD_FILE" ]; then
        set_property "postgresql-ssl-key-password" "`cat "$POSTGRES_SSL_KEY_PASSWORD_FILE"`"
    elif [ -n "$POSTGRES_SSL_KEY_PASSWORD" ]; then
        set_property "postgresql-ssl-key-password" "$POSTGRES_SSL_KEY_PASSWORD"
    fi

    # Add required .jar files to GUACAMOLE_LIB and GUACAMOLE_EXT
    ln -s /opt/guacamole/postgresql/postgresql-*.jar "$GUACAMOLE_LIB"
    ln -s /opt/guacamole/postgresql/guacamole-auth-*.jar "$GUACAMOLE_EXT"

}

##
## Adds properties to guacamole.properties which select the LDAP
## authentication provider, and configure it to connect to the specified LDAP
## directory.
##
associate_ldap() {

    # Verify required parameters are present
    if [ -z "$LDAP_HOSTNAME" -o -z "$LDAP_USER_BASE_DN" ]; then
        cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using an LDAP directory, you must provide each of the following environment
variables:

    LDAP_HOSTNAME      The hostname or IP address of your LDAP server.

    LDAP_USER_BASE_DN  The base DN under which all Guacamole users will be
                       located. Absolutely all Guacamole users that will
                       authenticate via LDAP must exist within the subtree of
                       this DN.
END
        exit 1;
    fi

    # Update config file
    set_property          "ldap-hostname"                   "$LDAP_HOSTNAME"
    set_property          "ldap-user-base-dn"               "$LDAP_USER_BASE_DN"

    set_optional_property "ldap-port"                       "$LDAP_PORT"
    set_optional_property "ldap-encryption-method"          "$LDAP_ENCRYPTION_METHOD"
    set_optional_property "ldap-max-search-results"         "$LDAP_MAX_SEARCH_RESULTS"
    set_optional_property "ldap-search-bind-dn"             "$LDAP_SEARCH_BIND_DN"
    set_optional_property "ldap-user-attributes"            "$LDAP_USER_ATTRIBUTES"
    set_optional_property "ldap-search-bind-password"       "$LDAP_SEARCH_BIND_PASSWORD"
    set_optional_property "ldap-username-attribute"         "$LDAP_USERNAME_ATTRIBUTE"
    set_optional_property "ldap-member-attribute"           "$LDAP_MEMBER_ATTRIBUTE"
    set_optional_property "ldap-user-search-filter"         "$LDAP_USER_SEARCH_FILTER"
    set_optional_property "ldap-config-base-dn"             "$LDAP_CONFIG_BASE_DN"
    set_optional_property "ldap-group-base-dn"              "$LDAP_GROUP_BASE_DN"
    set_optional_property "ldap-member-attribute-type"      "$LDAP_MEMBER_ATTRIBUTE_TYPE"
    set_optional_property "ldap-group-name-attribute"       "$LDAP_GROUP_NAME_ATTRIBUTE"
    set_optional_property "ldap-dereference-aliases"        "$LDAP_DEREFERENCE_ALIASES"
    set_optional_property "ldap-follow-referrals"           "$LDAP_FOLLOW_REFERRALS"
    set_optional_property "ldap-max-referral-hops"          "$LDAP_MAX_REFERRAL_HOPS"
    set_optional_property "ldap-operation-timeout"          "$LDAP_OPERATION_TIMEOUT"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/ldap/guacamole-auth-*.jar "$GUACAMOLE_EXT"

}

##
## Adds properties to guacamole.properties which select the LDAP
## authentication provider, and configure it to connect to the specified LDAP
## directory.
##
associate_radius() {

    # Verify required parameters are present
    if [ -z "$RADIUS_SHARED_SECRET" -o -z "$RADIUS_AUTH_PROTOCOL" ]; then
        cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using RADIUS server, you must provide each of the following environment
variables:

    RADIUS_SHARED_SECRET   The shared secret to use when talking to the 
                           RADIUS server.

    RADIUS_AUTH_PROTOCOL   The authentication protocol to use when talking 
                           to the RADIUS server.
                           Supported values are: 
                             pap, chap, mschapv1, mschapv2, eap-md5, 
                             eap-tls and eap-ttls.
END
        exit 1;
    fi

    # Verify provided files do exist and are readable
    if [ -n "$RADIUS_KEY_FILE" -a ! -r "$RADIUS_KEY_FILE" ]; then
       cat <<END
FATAL: Provided file RADIUS_KEY_FILE=$RADIUS_KEY_FILE does not exist 
       or is not readable!
-------------------------------------------------------------------------------
If you provide key or CA files you need to mount those into the container and
make sure they are readable for the user in the container.
END
        exit 1;
    fi
    if [ -n "$RADIUS_CA_FILE" -a ! -r "$RADIUS_CA_FILE" ]; then
       cat <<END
FATAL: Provided file RADIUS_CA_FILE=$RADIUS_CA_FILE does not exist 
       or is not readable!
-------------------------------------------------------------------------------
If you provide key or CA files you need to mount those into the container and
make sure they are readable for the user in the container.
END
        exit 1;
    fi
    if [ "$RADIUS_AUTH_PROTOCOL" = "eap-ttls" -a -z "$RADIUS_EAP_TTLS_INNER_PROTOCOL" ]; then
       cat <<END
FATAL: Authentication protocol "eap-ttls" specified but
       RADIUS_EAP_TTLS_INNER_PROTOCOL is not set!
-------------------------------------------------------------------------------
When EAP-TTLS is used, this parameter specifies the inner (tunneled)
protocol to use talking to the RADIUS server.
END
        exit 1;
    fi

    # Update config file
    set_optional_property "radius-hostname"         "$RADIUS_HOSTNAME"
    set_optional_property "radius-auth-port"        "$RADIUS_AUTH_PORT"
    set_property          "radius-shared-secret"    "$RADIUS_SHARED_SECRET"
    set_property          "radius-auth-protocol"    "$RADIUS_AUTH_PROTOCOL"
    set_optional_property "radius-key-file"         "$RADIUS_KEY_FILE"
    set_optional_property "radius-key-type"         "$RADIUS_KEY_TYPE"
    set_optional_property "radius-key-password"     "$RADIUS_KEY_PASSWORD"
    set_optional_property "radius-ca-file"          "$RADIUS_CA_FILE"
    set_optional_property "radius-ca-type"          "$RADIUS_CA_TYPE"
    set_optional_property "radius-ca-password"      "$RADIUS_CA_PASSWORD"
    set_optional_property "radius-trust-all"        "$RADIUS_TRUST_ALL"
    set_optional_property "radius-retries"          "$RADIUS_RETRIES"
    set_optional_property "radius-timeout"          "$RADIUS_TIMEOUT"

    set_optional_property \
       "radius-eap-ttls-inner-protocol" \
       "$RADIUS_EAP_TTLS_INNER_PROTOCOL"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/radius/guacamole-auth-*.jar "$GUACAMOLE_EXT"
}

## Adds properties to guacamole.properties which select the OPENID
## authentication provider, and configure it to connect to the specified OPENID
## provider.
##
associate_openid() {

    # Verify required parameters are present
    if [ -z "$OPENID_AUTHORIZATION_ENDPOINT" ] || \
       [ -z "$OPENID_JWKS_ENDPOINT" ]          || \
       [ -z "$OPENID_ISSUER" ]                 || \
       [ -z "$OPENID_CLIENT_ID" ]              || \          
       [ -z "$OPENID_REDIRECT_URI" ]
    then
        cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using an openid authentication, you must provide each of the following
environment variables:

    OPENID_AUTHORIZATION_ENDPOINT   The authorization endpoint (URI) of the OpenID service.

    OPENID_JWKS_ENDPOINT            The endpoint (URI) of the JWKS service which defines
                                    how received ID tokens (JSON Web Tokens or JWTs) 
                                    shall be validated.

    OPENID_ISSUER                   The issuer to expect for all received ID tokens.

    OPENID_CLIENT_ID                The OpenID client ID which should be submitted 
                                    to the OpenID service when necessary. 
                                    This value is typically provided to you by the OpenID 
                                    service when OpenID credentials are generated for your application.

    OPENID_REDIRECT_URI             The URI that should be submitted to the OpenID service such that 
                                    they can redirect the authenticated user back to Guacamole after 
                                    the authentication process is complete. This must be the full URL 
                                    that a user would enter into their browser to access Guacamole.
END
        exit 1;
    fi

    # Update config file
    set_property          "openid-authorization-endpoint"    "$OPENID_AUTHORIZATION_ENDPOINT"
    set_property          "openid-jwks-endpoint"             "$OPENID_JWKS_ENDPOINT"
    set_property          "openid-issuer"                    "$OPENID_ISSUER"
    set_property          "openid-client-id"                 "$OPENID_CLIENT_ID"
    set_property          "openid-redirect-uri"              "$OPENID_REDIRECT_URI"
    set_optional_property "openid-username-claim-type"       "$OPENID_USERNAME_CLAIM_TYPE"

    # Add required .jar files to GUACAMOLE_EXT
    # "1-{}" make it sorted as a first provider (only authentication)
    # so it can work together with the database providers (authorization)
    find /opt/guacamole/openid/ -name "*.jar" | awk -F/ '{print $NF}' | \
    xargs -I '{}' ln -s "/opt/guacamole/openid/{}" "${GUACAMOLE_EXT}/1-{}"

}

##
## Adds properties to guacamole.properties which configure the TOTP two-factor
## authentication mechanism.
##
associate_totp() {
    # Update config file
    set_optional_property "totp-issuer"    "$TOTP_ISSUER"
    set_optional_property "totp-digits"    "$TOTP_DIGITS"
    set_optional_property "totp-period"    "$TOTP_PERIOD"
    set_optional_property "totp-mode"      "$TOTP_MODE"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/totp/guacamole-auth-*.jar   "$GUACAMOLE_EXT"
}

##
## Adds properties to guacamole.properties which configure the Duo two-factor
## authentication service. Checks to see if all variables are defined and makes sure
## DUO_APPLICATION_KEY is >= 40 characters.
##
associate_duo() {
    # Verify required parameters are present
    if [ -z "$DUO_INTEGRATION_KEY" ]      || \
       [ -z "$DUO_SECRET_KEY" ]           || \
       [ ${#DUO_APPLICATION_KEY} -lt 40 ]
    then
        cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using the Duo authentication extension, you must provide each of the 
following environment variables:

    DUO_API_HOSTNAME        The hostname of the Duo API endpoint.

    DUO_INTEGRATION_KEY     The integration key provided for Guacamole by Duo.

    DUO_SECRET_KEY          The secret key provided for Guacamole by Duo. 

    DUO_APPLICATION_KEY     An arbitrary, random key.
                            This value must be at least 40 characters.
END
        exit 1;
    fi

    # Update config file
    set_property "duo-api-hostname"                 "$DUO_API_HOSTNAME"
    set_property "duo-integration-key"              "$DUO_INTEGRATION_KEY"
    set_property "duo-secret-key"                   "$DUO_SECRET_KEY"
    set_property "duo-application-key"              "$DUO_APPLICATION_KEY"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/duo/guacamole-auth-*.jar   "$GUACAMOLE_EXT"
}

##
## Adds properties to guacamole.properties which configure the header
## authentication provider.
##
associate_header() {
    # Update config file
    set_optional_property "http-auth-header"         "$HTTP_AUTH_HEADER"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/header/guacamole-auth-*.jar "$GUACAMOLE_EXT"
}

##
## Adds properties to guacamole.properties witch configure the CAS
## authentication service.
##
associate_cas() {
    # Verify required parameters are present
    if [ -z "$CAS_AUTHORIZATION_ENDPOINT" ] || \
       [ -z "$CAS_REDIRECT_URI" ]
    then
        cat <<END
FATAL: Missing required environment variables
-----------------------------------------------------------------------------------
If using the CAS authentication extension, you must provide each of the
following environment variables:

    CAS_AUTHORIZATION_ENDPOINT      The URL of the CAS authentication server.

    CAS_REDIRECT_URI                The URI to redirect back to upon successful authentication.

END
        exit 1;
    fi

    # Update config file
    set_property            "cas-authorization-endpoint"       "$CAS_AUTHORIZATION_ENDPOINT"
    set_property            "cas-redirect-uri"                 "$CAS_REDIRECT_URI"
    set_optional_property   "cas-clearpass-key"                "$CAS_CLEARPASS_KEY"
    set_optional_property   "cas-group-attribute"              "$CAS_GROUP_ATTRIBUTE"
    set_optional_property   "cas-group-format"                 "$CAS_GROUP_FORMAT"
    set_optional_property   "cas-group-ldap-base-dn"           "$CAS_GROUP_LDAP_BASE_DN"
    set_optional_property   "cas-group-ldap-attribute"         "$CAS_GROUP_LDAP_ATTRIBUTE"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/cas/guacamole-auth-*.jar   "$GUACAMOLE_EXT"
}

##
## Adds properties to guacamole.properties which configure the json
## authentication provider.
##
associate_json() {
    # Update config file
    set_property          "json-secret-key"        "$JSON_SECRET_KEY"
    set_optional_property "json-trusted-networks"  "$JSON_TRUSTED_NETWORKS"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/json/guacamole-auth-*.jar "$GUACAMOLE_EXT"
}

##
## Starts Guacamole under Tomcat, replacing the current process with the
## Tomcat process. As the current process will be replaced, this MUST be the
## last function run within the script.
##
start_guacamole() {

    # Install webapp
    rm -Rf /usr/local/tomcat/webapps/${WEBAPP_CONTEXT:-guacamole}
    ln -sf /opt/guacamole/guacamole.war /usr/local/tomcat/webapps/${WEBAPP_CONTEXT:-guacamole}.war

    # Start tomcat
    cd /usr/local/tomcat
    exec catalina.sh run

}

#
# Start with a fresh GUACAMOLE_HOME
#

rm -Rf "$GUACAMOLE_HOME"

#
# Copy contents of provided GUACAMOLE_HOME template, if any
#

if [ -n "$GUACAMOLE_HOME_TEMPLATE" ]; then
    cp -a "$GUACAMOLE_HOME_TEMPLATE/." "$GUACAMOLE_HOME/"
fi

#
# Create and define Guacamole lib and extensions directories
#

mkdir -p "$GUACAMOLE_EXT"
mkdir -p "$GUACAMOLE_LIB"

#
# Point to associated guacd
#

# Use linked container for guacd if specified
if [ -n "$GUACD_NAME" ]; then
    GUACD_HOSTNAME="$GUACD_PORT_4822_TCP_ADDR"
    GUACD_PORT="$GUACD_PORT_4822_TCP_PORT"
fi

# Use default guacd port if none specified
GUACD_PORT="${GUACD_PORT-4822}"

# Verify required guacd connection information is present
if [ -z "$GUACD_HOSTNAME" -o -z "$GUACD_PORT" ]; then
    cat <<END
FATAL: Missing GUACD_HOSTNAME or "guacd" link.
-------------------------------------------------------------------------------
Every Guacamole instance needs a corresponding copy of guacd running. To
provide this, you must either:

(a) Explicitly link that container with the link named "guacd".

(b) If not using a Docker container for guacd, explicitly specify the TCP
    connection information using the following environment variables:

GUACD_HOSTNAME     The hostname or IP address of guacd. If not using a guacd
                   Docker container and corresponding link, this environment
                   variable is *REQUIRED*.

GUACD_PORT         The port on which guacd is listening for TCP connections.
                   This environment variable is optional. If omitted, the
                   standard guacd port of 4822 will be used.
END
    exit 1;
fi

# Update config file
set_property "guacd-hostname" "$GUACD_HOSTNAME"
set_property "guacd-port"     "$GUACD_PORT"

#
# Track which authentication backends are installed
#

INSTALLED_AUTH=""

# Use MySQL if database specified
if [ -n "$MYSQL_DATABASE" -o -n "$MYSQL_DATABASE_FILE" ]; then
    associate_mysql
    INSTALLED_AUTH="$INSTALLED_AUTH mysql"
fi

# Use PostgreSQL if database specified
if [ -n "$POSTGRES_DATABASE" -o -n "$POSTGRES_DATABASE_FILE" ]; then
    associate_postgresql
    INSTALLED_AUTH="$INSTALLED_AUTH postgres"
fi

# Use LDAP directory if specified
if [ -n "$LDAP_HOSTNAME" ]; then
    associate_ldap
    INSTALLED_AUTH="$INSTALLED_AUTH ldap"
fi

# Use RADIUS server if specified
if [ -n "$RADIUS_SHARED_SECRET" ]; then
    associate_radius
    INSTALLED_AUTH="$INSTALLED_AUTH radius"
fi

# Use OPENID if specified
if [ -n "$OPENID_AUTHORIZATION_ENDPOINT" ]; then
    associate_openid
    INSTALLED_AUTH="$INSTALLED_AUTH openid"
fi

#
# Validate that at least one authentication backend is installed
#

if [ -z "$INSTALLED_AUTH" -a -z "$GUACAMOLE_HOME_TEMPLATE" ]; then
    cat <<END
FATAL: No authentication configured
-------------------------------------------------------------------------------
The Guacamole Docker container needs at least one authentication mechanism in
order to function, such as a MySQL database, PostgreSQL database, LDAP
directory or RADIUS server. Please specify at least the MYSQL_DATABASE or 
POSTGRES_DATABASE environment variables, or check Guacamole's Docker 
documentation regarding configuring LDAP and/or custom extensions.
END
    exit 1;
fi

# Use TOTP if specified.
if [ "$TOTP_ENABLED" = "true" ]; then
    associate_totp
fi

# Use Duo if specified.
if [ -n "$DUO_API_HOSTNAME" ]; then
    associate_duo
fi

# Use header if specified.
if [ "$HEADER_ENABLED" = "true" ]; then
    associate_header
fi

# Use CAS if specified.
if [ -n "$CAS_AUTHORIZATION_ENDPOINT" ]; then
    associate_cas
fi

# Use json-auth if specified.
if [ -n "$JSON_SECRET_KEY" ]; then
    associate_json
fi

# Set logback level if specified
if [ -n "$LOGBACK_LEVEL" ]; then
    unzip -o -j /opt/guacamole/guacamole.war WEB-INF/classes/logback.xml -d $GUACAMOLE_HOME
    sed -i "s/level=\"info\"/level=\"$LOGBACK_LEVEL\"/" $GUACAMOLE_HOME/logback.xml
fi

#
# Finally start Guacamole (under Tomcat)
#

start_guacamole

