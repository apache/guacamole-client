guacamole-noauth
================

Remove login screen from the `Guacamole <http://guac-dev.org/>`_ web interface.

Build
-----

- Download 

  ::

    git clone http://git.deltalima.net/guacamole-noauth/

- Compile

  ::

    cd extensions/guacamole-auth-noauth/
    mvn package

  These will create a new jar file `guacamole-noauth-VERSION.jar` in the
  `target/` folder.


Install
-------

- Copy `guacamole-noauth-VERSION.jar` in `webapps/guacamole/WEB-INF/lib/`. It
  does not work if you copy the jar in `common/lib/` or `shared/lib/`.

Configure
---------

- Edit the Guacamole configuration file (`/etc/guacamole/guacamole.properties`):

  ::

    # Hostname and port of guacamole proxy
    guacd-hostname: localhost
    guacd-port:     4822

    auth-provider: net.sourceforge.guacamole.net.auth.noauth.NoAuthenticationProvider
    noauth-config: /etc/guacamole/noauth-config.xml

- Create a new file `/etc/gacamole/noauth-config.xml`:

  ::

    <configs>
      <config name="my-rdp-server" protocol="rdp">
        <param name="hostname" value="my-rdp-server-hostname" />
        <param name="port" value="3389" />
      </config>
    </configs>

- Restart Tomcat
