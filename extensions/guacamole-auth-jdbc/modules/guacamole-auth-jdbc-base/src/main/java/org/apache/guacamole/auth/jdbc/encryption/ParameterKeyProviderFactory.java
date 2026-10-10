/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.jdbc.encryption;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;

/**
 * Creates a ParameterKeyProvider. Implementations are discovered with
 * ServiceLoader and selected by the "*-parameter-encryption-provider"
 * property.
 */
public interface ParameterKeyProviderFactory {

    /**
     * Returns the name selecting this factory, such as "property".
     *
     * @return
     *     The name of this factory.
     */
    String getName();

    /**
     * Creates the provider from the given environment.
     *
     * @param environment
     *     The environment supplying the provider's configuration.
     *
     * @return
     *     The new provider.
     *
     * @throws GuacamoleException
     *     If the configuration is invalid.
     */
    ParameterKeyProvider create(JDBCEnvironment environment)
            throws GuacamoleException;

    /**
     * Returns the configured provider, or PropertyParameterKeyProvider.DISABLED
     * if no keys are configured.
     *
     * @param environment
     *     The environment supplying the configuration.
     *
     * @return
     *     The configured provider.
     *
     * @throws GuacamoleException
     *     If the configuration is invalid or names an unknown provider.
     */
    static ParameterKeyProvider loadConfigured(JDBCEnvironment environment)
            throws GuacamoleException {

        if (environment.getParameterEncryptionKeys() == null)
            return PropertyParameterKeyProvider.DISABLED;

        String name = environment.getParameterEncryptionProvider();
        List<String> available = new ArrayList<>();
        for (ParameterKeyProviderFactory factory : ServiceLoader.load(
                ParameterKeyProviderFactory.class,
                ParameterKeyProviderFactory.class.getClassLoader())) {
            if (factory.getName().equals(name))
                return factory.create(environment);
            available.add(factory.getName());
        }

        throw new GuacamoleServerException("Unknown parameter encryption "
                + "provider \"" + name + "\". Available: " + available);

    }

}
