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

package org.apache.guacamole.rest.work;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.Work;
import org.apache.guacamole.rest.directory.DirectoryObjectTranslator;

public class WorkObjectTranslator extends DirectoryObjectTranslator<Work, APIWork> {
    
    @Override
    public APIWork toExternalObject(Work object)
            throws GuacamoleException {
        return new APIWork(object);
    }

    @Override
    public Work toInternalObject(APIWork object)
            throws GuacamoleException {
        return new APIWorkWrapper(object);
    }

    @Override
    public void applyExternalChanges(Work existingObject,
            APIWork object) throws GuacamoleException {

        // Update user attributes
        existingObject.setAttributes(object.getAttributes());

    }

    @Override
    public void filterExternalObject(UserContext userContext, APIWork object)
            throws GuacamoleException {

        // Filter object attributes by defined schema
        object.setAttributes(filterAttributes(userContext.getWorkAttributes(),
                object.getAttributes()));

    }

}
