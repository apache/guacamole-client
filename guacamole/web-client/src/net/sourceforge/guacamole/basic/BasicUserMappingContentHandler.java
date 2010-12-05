
package net.sourceforge.guacamole.basic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static class AuthInfo {

        public static enum Encoding {
            PLAIN_TEXT,
            MD5
        }

        private String auth_username;
        private String auth_password;
        private Encoding auth_encoding;

        private String protocol;
        private String hostname;
        private int port;
        private String password;

        public AuthInfo(String auth_username, String auth_password, Encoding auth_encoding) {
            this.auth_username = auth_username;
            this.auth_password = auth_password;
            this.auth_encoding = auth_encoding;
        }

        private static final char HEX_CHARS[] = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };

        public static String getHexString(byte[] bytes) {

            if (bytes == null)
                return null;

            StringBuilder hex = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                hex.append(HEX_CHARS[(b & 0xF0) >> 4])
                   .append(HEX_CHARS[(b & 0x0F)     ]);
            }

            return hex.toString();

        }


        public boolean validate(String username, String password) {

            // If username matches
            if (username != null && password != null && username.equals(auth_username)) {

                switch (auth_encoding) {

                    case PLAIN_TEXT:

                        // Compare plaintext
                        return password.equals(auth_password);
                    
                    case MD5:

                        // Compare hashed password
                        try {
                            MessageDigest digest = MessageDigest.getInstance("MD5");
                            String hashedPassword = getHexString(digest.digest(password.getBytes()));
                            return hashedPassword.equals(auth_password.toUpperCase());
                        }
                        catch (NoSuchAlgorithmException e) {
                            throw new UnsupportedOperationException("Unexpected lack of MD5 support.", e);
                        }
                    
                }
                
            }

            return false;
            
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
                current.auth_username,
                current
            );

        }

        infoState = null;

    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (localName.equals("authorize")) {

            AuthInfo.Encoding encoding;
            String encodingString = attributes.getValue("encoding");
            if (encodingString == null)
                encoding = AuthInfo.Encoding.PLAIN_TEXT;
            else if (encodingString.equals("plain"))
                encoding = AuthInfo.Encoding.PLAIN_TEXT;
            else if (encodingString.equals("md5"))
                encoding = AuthInfo.Encoding.MD5;
            else
                throw new SAXException("Invalid encoding type");


            current = new AuthInfo(
                attributes.getValue("username"),
                attributes.getValue("password"),
                encoding
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
