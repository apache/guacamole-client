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

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;
import org.apache.guacamole.net.auth.ActivityRecord;

/**
 * An ActivityRecord which is backed by a database model.
 */
public class ModeledActivityRecord implements ActivityRecord {

    /**
     * The model object backing this activity record.
     */
    private final ActivityRecordModel model;

    /**
     * The UUID uniquely identifies this record, or null if no such unique
     * identifier exists.
     */
    private final UUID uuid;

    /**
     * Generates a UUID that uniquely identifies the record represented by the
     * given ActivityRecordModel. The UUID generated is a type 3 name UUID and
     * is guaranteed to be unique so long as the provided UUID namespace
     * corresponds to the namespace of the record ID within the model.
     * <p>
     * IMPORTANT: Changing this function such that different UUIDs will be
     * generated for the same records relative to past releases can potentially
     * break compatibility with established history record associations. Any
     * such change should be made with great care to avoid breaking history
     * functionality that may be provided by third-party extensions.
     *
     * @param namespace
     *     The UUID namespace of the type 3 name UUID to generate. This
     *     namespace should correspond to the source of IDs for the model
     *     such that the combination of this namespace with the numeric record
     *     ID will always be unique and deterministic across all activity
     *     records, regardless of record type.
     *
     * @param model
     *     The model object representing the activity record.
     *
     * @return
     *     The UUID uniquely identifies the record represented by the given
     *     model, or null if no such unique identifier can be generated (there
     *     is no corresponding record ID).
     */
    private static UUID getUUID(UUID namespace, ActivityRecordModel model) {

        Integer id = model.getRecordID();
        if (id == null)
            return null;

        // Convert record ID to a name UUID in the given namespace
        return UUID.nameUUIDFromBytes(ByteBuffer.allocate(24)
                .putLong(namespace.getMostSignificantBits())
                .putLong(namespace.getLeastSignificantBits())
                .putLong(id)
                .array());

    }

    /**
     * Creates a new ModeledActivityRecord backed by the given model object.
     * Changes to this record will affect the backing model object, and changes
     * to the backing model object will affect this record.
     *
     * @param namespace
     *     The UUID namespace of the type 3 name UUID to generate for the
     *     record. This namespace should correspond to the source of IDs for
     *     the model such that the combination of this namespace with the
     *     numeric record ID will always be unique and deterministic across all
     *     activity records, regardless of record type.
     *
     * @param model
     *     The model object to use to back this activity record.
     */
    public ModeledActivityRecord(UUID namespace, ActivityRecordModel model) {
        this.model = model;
        this.uuid = getUUID(namespace, model);
    }

    @Override
    public Date getStartDate() {
        return model.getStartDate();
    }

    @Override
    public Date getEndDate() {
        return model.getEndDate();
    }

    @Override
    public String getRemoteHost() {
        return model.getRemoteHost();
    }

    @Override
    public String getUsername() {
        return model.getUsername();
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }
    
}
