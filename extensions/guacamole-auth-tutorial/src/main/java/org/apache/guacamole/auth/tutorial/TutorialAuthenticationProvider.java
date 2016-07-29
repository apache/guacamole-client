package org.apache.guacamole.auth.tutorial;

import java.util.HashMap;
import java.util.Map;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * Authentication provider implementation intended to demonstrate basic use
 * of Guacamole's extension API. The credentials and connection information for
 * a single user are stored directly in guacamole.properties.
 */
public class TutorialAuthenticationProvider extends SimpleAuthenticationProvider {

    @Override
    public String getIdentifier() {
        return "tutorial";
    }


    @Override
    public Map<String, GuacamoleConfiguration>
        getAuthorizedConfigurations(Credentials credentials)
        throws GuacamoleException {

        // Get the Guacamole server environment
        Environment environment = new LocalEnvironment();

        // Get username from guacamole.properties
        String username = environment.getRequiredProperty(
            TutorialGuacamoleProperties.TUTORIAL_USER
        );      

        // If wrong username, fail
        if (!username.equals(credentials.getUsername()))
            return null;

        // Get password from guacamole.properties
        String password = environment.getRequiredProperty(
            TutorialGuacamoleProperties.TUTORIAL_PASSWORD
        );      

        // If wrong password, fail
        if (!password.equals(credentials.getPassword()))
            return null;

        // Successful login. Return configurations.
        Map<String, GuacamoleConfiguration> configs = 
            new HashMap<String, GuacamoleConfiguration>();

        // Create new configuration
        GuacamoleConfiguration config = new GuacamoleConfiguration();

        // Set protocol specified in properties
        config.setProtocol(environment.getRequiredProperty(
            TutorialGuacamoleProperties.TUTORIAL_PROTOCOL
        ));

        // Set all parameters, splitting at commas
        for (String parameterValue : environment.getRequiredProperty(
            TutorialGuacamoleProperties.TUTORIAL_PARAMETERS
        ).split(",\\s*")) {

            // Find the equals sign
            int equals = parameterValue.indexOf('=');
            if (equals == -1)
                throw new GuacamoleServerException("Required equals sign missing");

            // Get name and value from parameter string
            String name = parameterValue.substring(0, equals);
            String value = parameterValue.substring(equals+1);

            // Set parameter as specified
            config.setParameter(name, value);

        }

        configs.put("Tutorial Connection", config);
        return configs;

    }    
}