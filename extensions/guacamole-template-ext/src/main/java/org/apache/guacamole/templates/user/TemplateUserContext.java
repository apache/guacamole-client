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

package org.apache.guacamole.templates.user;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.DecoratingDirectory;
import org.apache.guacamole.net.auth.DelegatingUserContext;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.templates.connection.TemplatedConnection;

/**
 * A UserContext which decorates another context, adding the capability for
 * connections within this context to be linked to other connections in order
 * to inherit settings from those connections.
 */
public class TemplateUserContext extends DelegatingUserContext {
    
    /**
     * Create a new instances of the TemplateUserContext, wrapping the given
     * UserContext and delegating functionality to that context.
     * 
     * @param context 
     *     The UserContext to wrap.
     */
    public TemplateUserContext(UserContext context) {
        super(context);
    }
    
    @Override
    public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
        return new DecoratingDirectory<Connection>(super.getConnectionDirectory()) {
            
            @Override
            protected Connection decorate(Connection object) {
                if (object instanceof TemplatedConnection)
                    return object;
                return new TemplatedConnection(object, this);
            }
            
            @Override
            protected Connection undecorate(Connection object) {
                if (object instanceof TemplatedConnection)
                    return ((TemplatedConnection) object).getWrappedConnection();
                return object;
            }
            
        };
    }
    
    @Override
    public Collection<Form> getConnectionAttributes() {
        Collection<Form> connectionAttrs = new HashSet<>(super.getConnectionAttributes());
        connectionAttrs.add(TemplatedConnection.TEMPLATED_CONNECTION_FORM);
        return Collections.unmodifiableCollection(connectionAttrs);
    }
    
}
