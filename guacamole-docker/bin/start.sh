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

    # Verify required parameters are present
    if [ -z "$MYSQL_USER" -o -z "$MYSQL_PASSWORD" -o -z "$MYSQL_DATABASE" ]; then
        cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using a MySQL database, you must provide each of the following
environment variables:

    MYSQL_USER         The user to authenticate as when connecting to
                       MySQL.

    MYSQL_PASSWORD     The password to use when authenticating with MySQL as
                       MYSQL_USER.

    MYSQL_DATABASE     The name of the MySQL database to use for Guacamole
                       authentication.
END
        exit 1;
    fi

    # Update config file
    set_property "mysql-hostname" "$MYSQL_HOSTNAME"
    set_property "mysql-port"     "$MYSQL_PORT"
    set_property "mysql-database" "$MYSQL_DATABASE"
    set_property "mysql-username" "$MYSQL_USER"
    set_property "mysql-password" "$MYSQL_PASSWORD"

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

    # Add required .jar files to GUACAMOLE_LIB and GUACAMOLE_EXT
    ln -s /opt/guacamole/mysql/mysql-connector-*.jar "$GUACAMOLE_LIB"
    ln -s /opt/guacamole/mysql/guacamole-auth-*.jar "$GUACAMOLE_EXT"

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

    # Verify required parameters are present
    if [ -z "$POSTGRES_USER" -o -z "$POSTGRES_PASSWORD" -o -z "$POSTGRES_DATABASE" ]; then
        cat <<END
FATAL: Missing required environment variables
-------------------------------------------------------------------------------
If using a PostgreSQL database, you must provide each of the following
environment variables:

    POSTGRES_USER      The user to authenticate as when connecting to
                       PostgreSQL.

    POSTGRES_PASSWORD  The password to use when authenticating with PostgreSQL
                       as POSTGRES_USER.

    POSTGRES_DATABASE  The name of the PostgreSQL database to use for Guacamole
                       authentication.
END
        exit 1;
    fi

    # Update config file
    set_property "postgresql-hostname" "$POSTGRES_HOSTNAME"
    set_property "postgresql-port"     "$POSTGRES_PORT"
    set_property "postgresql-database" "$POSTGRES_DATABASE"
    set_property "postgresql-username" "$POSTGRES_USER"
    set_property "postgresql-password" "$POSTGRES_PASSWORD"

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
    set_property          "ldap-hostname"           "$LDAP_HOSTNAME"
    set_optional_property "ldap-port"               "$LDAP_PORT"
    set_optional_property "ldap-encryption-method"  "$LDAP_ENCRYPTION_METHOD"
    set_property          "ldap-user-base-dn"       "$LDAP_USER_BASE_DN"
    set_optional_property "ldap-username-attribute" "$LDAP_USERNAME_ATTRIBUTE"
    set_optional_property "ldap-group-base-dn"      "$LDAP_GROUP_BASE_DN"
    set_optional_property "ldap-config-base-dn"     "$LDAP_CONFIG_BASE_DN"

    set_optional_property     \
        "ldap-search-bind-dn" \
        "$LDAP_SEARCH_BIND_DN"

    set_optional_property           \
        "ldap-search-bind-password" \
        "$LDAP_SEARCH_BIND_PASSWORD"

    # Add required .jar files to GUACAMOLE_EXT
    ln -s /opt/guacamole/ldap/guacamole-auth-*.jar "$GUACAMOLE_EXT"

}

##
## Starts Guacamole under Tomcat, replacing the current process with the
## Tomcat process. As the current process will be replaced, this MUST be the
## last function run within the script.
##
start_guacamole() {

    # Install webapp
    ln -sf /opt/guacamole/guacamole.war /usr/local/tomcat/webapps/

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
if [ -n "$MYSQL_DATABASE" ]; then
    associate_mysql
    INSTALLED_AUTH="$INSTALLED_AUTH mysql"
fi

# Use PostgreSQL if database specified
if [ -n "$POSTGRES_DATABASE" ]; then
    associate_postgresql
    INSTALLED_AUTH="$INSTALLED_AUTH postgres"
fi

# Use LDAP directory if specified
if [ -n "$LDAP_HOSTNAME" ]; then
    associate_ldap
    INSTALLED_AUTH="$INSTALLED_AUTH ldap"
fi

#
# Validate that at least one authentication backend is installed
#

if [ -z "$INSTALLED_AUTH" -a -z "$GUACAMOLE_HOME_TEMPLATE" ]; then
    cat <<END
FATAL: No authentication configured
-------------------------------------------------------------------------------
The Guacamole Docker container needs at least one authentication mechanism in
order to function, such as a MySQL database, PostgreSQL database, or LDAP
directory.  Please specify at least the MYSQL_DATABASE or POSTGRES_DATABASE
environment variables, or check Guacamole's Docker documentation regarding
configuring LDAP and/or custom extensions.
END
    exit 1;
fi

#
# Finally start Guacamole (under Tomcat)
#

start_guacamole

