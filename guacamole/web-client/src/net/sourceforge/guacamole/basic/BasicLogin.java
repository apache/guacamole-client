
package net.sourceforge.guacamole.basic;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.basic.BasicUserMappingContentHandler.AuthInfo;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class BasicLogin extends HttpServlet {

    private long mappingTime;
    private Map<String, AuthInfo> mapping;

    // Added to session when session validated
    public class AuthorizedConfiguration {

        private String protocol;
        private String hostname;
        private int port;
        private String password;

        public AuthorizedConfiguration(String protocol, String hostname, int port, String password) {
            this.protocol = protocol;
            this.hostname = hostname;
            this.port = port;
            this.password = password;
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

    private File getUserMappingFile() {
        
        // Get user mapping filename
        ServletContext context = getServletContext();
        String filename = context.getInitParameter("basic-user-mapping");
        if (filename == null)
            return null;

        return new File(filename);

    }

    @Override
    public synchronized void init() throws ServletException {

        // Get user mapping file
        File mapFile = getUserMappingFile();
        if (mapFile == null)
            throw new ServletException("Missing \"basic-user-mapping\" parameter required for basic login.");

        // Parse document
        try {

            BasicUserMappingContentHandler contentHandler = new BasicUserMappingContentHandler();

            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(contentHandler);
            parser.parse(mapFile.getAbsolutePath());

            mappingTime = mapFile.lastModified();
            mapping = contentHandler.getUserMapping();

        }
        catch (IOException e) {
            throw new ServletException("Error reading basic user mapping file.", e);
        }
        catch (SAXException e) {
            throw new ServletException("Error parsing basic user mapping XML.", e);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Check mapping file mod time
        File userMappingFile = getUserMappingFile();
        if (userMappingFile.exists() && mappingTime < userMappingFile.lastModified()) {

            // If modified recently, gain exclusive access and recheck
            synchronized (this) {
                if (userMappingFile.exists() && mappingTime < userMappingFile.lastModified())
                    init(); // If still not up to date, re-init
            }

        }

        // Retrieve username and password from parms
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Retrieve corresponding authorization info
        AuthInfo info = mapping.get(username);
        if (info != null) {

            // Validate username and password
            if (info.validate(username, password)) {

                // Store authorized configuration
                HttpSession session = req.getSession(true);
                session.setAttribute(
                    "BASIC-LOGIN-AUTH",
                    new AuthorizedConfiguration(
                        info.getProtocol(),
                        info.getHostname(),
                        info.getPort(),
                        info.getPassword()
                    )
                );
                
                // Success
                return;

            }
            
        }

        // Report "forbidden" on any failure
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Login invalid");

    }


}
