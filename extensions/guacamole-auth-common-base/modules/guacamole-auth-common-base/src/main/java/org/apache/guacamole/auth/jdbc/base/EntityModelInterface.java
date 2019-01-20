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

/**
 * Base representation of a Guacamole object that can be granted permissions
 * (an "entity"), such as a user or user group, as represented in the database.
 * Each entity has three base properties:
 *
 *   1. The "entityID", which points to the common entry in the
 *      guacamole_entity table and is common to any type of entity.
 *
 *   2. The "objectID", which points to the type-specific entry for the object
 *      in question (ie: an entry in guacamole_user or guacamole_user_group).
 *
 *   3. The "identifier", which contains the unique "name" value defined for
 *      the entity within the guacamole_entity table.
 */
public interface EntityModelInterface {

	public Integer getEntityID();
	
	public EntityType getEntityType();

}
