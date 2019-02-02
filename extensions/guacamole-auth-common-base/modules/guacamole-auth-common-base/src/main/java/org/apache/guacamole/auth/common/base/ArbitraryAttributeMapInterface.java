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

package org.apache.guacamole.auth.common.base;

import java.util.Collection;
import java.util.Map;

/**
 * Map of arbitrary attribute name/value pairs which can alternatively be
 * exposed as a collection of model objects.
 */
public interface ArbitraryAttributeMapInterface extends Map<String, String> {

    /**
     * Returns a collection of model objects which mirrors the contents of this
     * ArbitraryAttributeMap. Each name/value pair within the map is reflected
     * by a corresponding model object within the returned collection. Removing
     * a model object from the collection removes the corresponding name/value
     * pair from the map. Adding a new model object to the collection adds a
     * corresponding name/value pair to the map. Changes to a model object
     * within the collection are NOT reflected on the map, however.
     *
     * @return A collection of model objects which mirrors the contents of this
     *         ArbitraryAttributeMap.
     */
    public Collection<ArbitraryAttributeModelInterface> toModelCollection();

}
