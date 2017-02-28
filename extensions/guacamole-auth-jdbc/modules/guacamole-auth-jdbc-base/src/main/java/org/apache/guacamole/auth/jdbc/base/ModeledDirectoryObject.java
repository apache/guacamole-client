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

package org.apache.guacamole.auth.jdbc.base;

import org.apache.guacamole.net.auth.Identifiable;

/**
 * Common base class for objects that will ultimately be made available through
 * the Directory class and are persisted to an underlying database model. All
 * such objects will need the same base set of queries to fulfill the needs of
 * the Directory class.
 *
 * @param <ModelType>
 *     The type of model object that corresponds to this object.
 */
public abstract class ModeledDirectoryObject<ModelType extends ObjectModel>
    extends ModeledObject<ModelType> implements Identifiable {

    @Override
    public String getIdentifier() {
        return getModel().getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        getModel().setIdentifier(identifier);
    }

}
