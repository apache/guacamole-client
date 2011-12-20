
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.GuacamoleTunnel;
import net.sourceforge.guacamole.net.InetGuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import net.sourceforge.guacamole.protocol.ConfiguredGuacamoleSocket;
import net.sourceforge.guacamole.servlet.GuacamoleSession;
import net.sourceforge.guacamole.servlet.GuacamoleHTTPTunnelServlet;

public class ExampleTunnelServlet extends GuacamoleHTTPTunnelServlet {

    @Override
    protected GuacamoleTunnel doConnect(HttpServletRequest request)
        throws GuacamoleException {

        HttpSession httpSession = request.getSession(true);

        String hostname = GuacamoleProperties.getProperty(
            GuacamoleProperties.GUACD_HOSTNAME);

        int port = GuacamoleProperties.getProperty(
                GuacamoleProperties.GUACD_PORT);

        GuacamoleConfiguration config = new GuacamoleConfiguration();
        config.setProtocol("vnc");
        config.setParameter("hostname", "localhost");
        config.setParameter("port", "5901");
        config.setParameter("password", "potato");

        GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                new InetGuacamoleSocket(hostname, port),
                config
        );

        GuacamoleTunnel tunnel = new GuacamoleTunnel(socket);

        // Attach tunnel
        GuacamoleSession session = new GuacamoleSession(httpSession);
        session.attachTunnel(tunnel);

        return tunnel;

    }

}
