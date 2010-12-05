
package net.sourceforge.guacamole.basic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BasicUserMappingContentHandler extends DefaultHandler {

    private Map<String, AuthInfo> authMapping = new HashMap<String, AuthInfo>();

    public Map<String, AuthInfo> getUserMapping() {
        return Collections.unmodifiableMap(authMapping);
    }

    public class AuthInfo {

        private String auth_username;
        private String auth_password;

        private String protocol;
        private String hostname;
        private int port;
        private String password;

        public AuthInfo(String auth_username, String auth_password) {
            this.auth_username = auth_username;
            this.auth_password = auth_password;
        }

        public String getAuthorizedUsername() {
            return auth_username;
        }

        public String getAuthorizedPassword() {
            return auth_password;
        }
        
        public String getHostname() {
            return hostname;
        }

        public String getPassword() {
            return password;
        }

        public int getPort() {
            return port;
        }

        public String getProtocol() {
            return protocol;
        }

    }

    private AuthInfo current;

    private enum AUTH_INFO_STATE {
        PROTOCOL,
        HOSTNAME,
        PORT,
        PASSWORD
    };

    private AUTH_INFO_STATE infoState;

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (localName.equals("authorize")) {

            // Finalize mapping for this user
            authMapping.put(
                current.getAuthorizedUsername(),
                current
            );

        }

        infoState = null;

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (localName.equals("authorize")) {

            current = new AuthInfo(
                attributes.getValue("username"),
                attributes.getValue("password")
            );

            infoState = null;

        }

        else if (localName.equals("protocol"))
            infoState = AUTH_INFO_STATE.PROTOCOL;

        else if (localName.equals("hostname"))
            infoState = AUTH_INFO_STATE.HOSTNAME;

        else if (localName.equals("port"))
            infoState = AUTH_INFO_STATE.PORT;

        else if (localName.equals("password"))
            infoState = AUTH_INFO_STATE.PASSWORD;

        else
            infoState = null;

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        String str = new String(ch, start, length);

        if (infoState == null)
            return;

        switch (infoState) {

            case PROTOCOL:
                current.protocol = str;
                break;

            case HOSTNAME:
                current.hostname = str;
                break;

            case PORT:
                current.port = Integer.parseInt(str);
                break;

            case PASSWORD:
                current.password = str;
                break;

        }

    }


}
