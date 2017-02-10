# guacamole-auth-cas
CAS SSO Module for Guacamole

This is an extension module for Guacamole that authenticates using a CAS SSO backend.

There are two parameters to add to the guacamole.properties file:
- cas-authorization-endpoint -> The URL of the CAS server.
- cas-redirect-uri -> Where the CAS server should redirect back to after a successful login.

This module was written using source code provided by Mike Jumper for the OpenID (OAUTH) module and modified to fit the CAS authentication process.
