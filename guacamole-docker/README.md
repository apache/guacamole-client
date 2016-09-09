What is Apache Guacamole?
=========================

[Apache Guacamole](http://guacamole.incubator.apache.org/) is a clientless
remote desktop gateway. It supports standard protocols like VNC and RDP. We
call it clientless because no plugins or client software are required.

Thanks to HTML5, once Guacamole is installed on a server, all you need to
access your desktops is a web browser.

How to use this image
=====================

Using this image will require an existing, running Docker container with the
[guacd image](https://registry.hub.docker.com/u/glyptodon/guacd/), and either
network access to a working LDAP server, or another Docker container providing
a PostgreSQL or MySQL database.

The name of the database and all associated credentials are specified with
environment variables given when the container is created. All other
configuration information is generated from the Docker links.

Beware that you will need to initialize the database manually. Guacamole will
not automatically create its own tables, but SQL scripts are provided to do
this.

Once the Guacamole image is running, Guacamole will be accessible at
`http://[address of container]:8080/guacamole/`. The instructions below use the
`-p 8080:8080` option to expose this port at the level of the machine hosting
Docker, as well.

Deploying Guacamole with PostgreSQL authentication
--------------------------------------------------

    docker run --name some-guacamole --link some-guacd:guacd \
        --link some-postgres:postgres      \
        -e POSTGRES_DATABASE=guacamole_db  \
        -e POSTGRES_USER=guacamole_user    \
        -e POSTGRES_PASSWORD=some_password \
        -d -p 8080:8080 glyptodon/guacamole

Linking Guacamole to PostgreSQL requires three environment variables. If any of
these environment variables are omitted, you will receive an error message, and
the image will stop:

1. `POSTGRES_DATABASE` - The name of the database to use for Guacamole authentication.
2. `POSTGRES_USER` - The user that Guacamole will use to connect to PostgreSQL.
3. `POSTGRES_PASSWORD` - The password that Guacamole will provide when connecting to PostgreSQL as `POSTGRES_USER`.

### Initializing the PostgreSQL database

If your database is not already initialized with the Guacamole schema, you will
need to do so prior to using Guacamole. A convenience script for generating the
necessary SQL to do this is included in the Guacamole image.

To generate a SQL script which can be used to initialize a fresh PostgreSQL
database
[as documented in the Guacamole manual](http://guacamole.incubator.apache.org/doc/gug/jdbc-auth.html#jdbc-auth-postgresql):

    docker run --rm glyptodon/guacamole /opt/guacamole/bin/initdb.sh --postgres > initdb.sql

Alternatively, you can use the SQL scripts included with
[guacamole-auth-jdbc](http://sourceforge.net/projects/guacamole/files/current/extensions/guacamole-auth-jdbc-0.9.6.tar.gz/download).

Once this script is generated, you must:

1. Create a database for Guacamole within PostgreSQL, such as `guacamole_db`.
2. Run the script on the newly-created database.
3. Create a user for Guacamole within PostgreSQL with access to the tables and
   sequences of this database, such as `guacamole_user`.

The process for doing this via the `psql` and `createdb` utilities included
with PostgreSQL is documented in
[the Guacamole manual](http://guacamole.incubator.apache.org/doc/gug/jdbc-auth.html#jdbc-auth-postgresql).

Deploying Guacamole with MySQL authentication
--------------------------------------------------

    docker run --name some-guacamole --link some-guacd:guacd \
        --link some-mysql:mysql         \
        -e MYSQL_DATABASE=guacamole_db  \
        -e MYSQL_USER=guacamole_user    \
        -e MYSQL_PASSWORD=some_password \
        -d -p 8080:8080 glyptodon/guacamole

Linking Guacamole to MySQL requires three environment variables. If any of
these environment variables are omitted, you will receive an error message, and
the image will stop:

1. `MYSQL_DATABASE` - The name of the database to use for Guacamole authentication.
2. `MYSQL_USER` - The user that Guacamole will use to connect to MySQL.
3. `MYSQL_PASSWORD` - The password that Guacamole will provide when connecting to MySQL as `MYSQL_USER`.

### Initializing the MySQL database

If your database is not already initialized with the Guacamole schema, you will
need to do so prior to using Guacamole. A convenience script for generating the
necessary SQL to do this is included in the Guacamole image.

To generate a SQL script which can be used to initialize a fresh MySQL database
[as documented in the Guacamole manual](http://guacamole.incubator.apache.org/doc/gug/jdbc-auth.html#jdbc-auth-mysql):

    docker run --rm glyptodon/guacamole /opt/guacamole/bin/initdb.sh --mysql > initdb.sql

Alternatively, you can use the SQL scripts included with
[guacamole-auth-jdbc](http://sourceforge.net/projects/guacamole/files/current/extensions/guacamole-auth-jdbc-0.9.6.tar.gz/download).

Once this script is generated, you must:

1. Create a database for Guacamole within MySQL, such as `guacamole_db`.
2. Create a user for Guacamole within MySQL with access to this database, such
   as `guacamole_user`.
3. Run the script on the newly-created database.

The process for doing this via the `mysql` utility included with MySQL is
documented in
[the Guacamole manual](http://guacamole.incubator.apache.org/doc/gug/jdbc-auth.html#jdbc-auth-mysql).


Deploying Apache Guacamole with LDAP authentication
---------------------------------------------------

    docker run --name some-guacamole --link some-guacd:guacd    \
        -e LDAP_HOSTNAME=172.17.42.1                            \
        -e LDAP_USER_BASE_DN=ou=people,dc=example,dc=com        \
        -e LDAP_CONFIG_BASE_DN=ou=connections,dc=example,dc=com \
        -d -p 8080:8080 glyptodon/guacamole

Using Guacamole with your LDAP directory will require additional configuration parameters
specified via environment variables. These variables collectively describe how Guacamole
will connect to LDAP:

1. `LDAP_HOSTNAME` - The hostname or IP address of your LDAP server.
2. `LDAP_USER_BASE_DN` - The base of the DN for all Guacamole users.
3. `LDAP_PORT` - The port your LDAP server listens on.  (Optional)
4. `LDAP_ENCRYPTION_METHOD` - The encryption mechanism that Guacamole should use when
communicating with your LDAP server. (Optional)
5. `LDAP_GROUP_BASE_DN` - The base of the DN for all groups that may be
referenced within Guacamole configurations using the standard `seeAlso` attribute. (Optional)
6. `LDAP_SEARCH_BIND_DN` - The DN (Distinguished Name) of the user to bind as when
authenticating users that are attempting to log in. (Optional)
7. `LDAP_SEARCH_BIND_PASSWORD` - The password to provide to the LDAP server when
binding as `LDAP_SEARCH_BIND_DN` to authenticate other users. (Optional)
8. `LDAP_USERNAME_ATTRIBUTE` - The attribute or attributes which contain the
username within all Guacamole user objects in the LDAP directory. (Optional)
9. `LDAP_CONFIG_BASE_DN` - The base of the DN for all Guacamole configurations. (Optional)

### Deploying Apache Guacamole with LDAP and database authentication

    docker run --name some-guacamole --link some-guacd:guacd    \
        --link some-mysql:mysql                                 \
        -e LDAP_HOSTNAME=172.17.42.1                            \
        -e LDAP_USER_BASE_DN=ou=people,dc=example,dc=com        \
        -e LDAP_CONFIG_BASE_DN=ou=connections,dc=example,dc=com \
        -e MYSQL_DATABASE=guacamole_db                          \
        -e MYSQL_USER=guacamole_user                            \
        -e MYSQL_PASSWORD=some_password                         \
        -d -p 8080:8080 glyptodon/guacamole

Guacamole does support combining LDAP with a MySQL or PostgreSQL database, and this
can be configured with the Guacamole Docker image, as well. By providing the
required environment variables for both systems, Guacamole will automatically be
configured to use both when the Docker image starts.

Reporting issues
================

Please report any bugs encountered by opening a new issue in
[our JIRA](https://issues.apache.org/jira/browse/GUACAMOLE/).

