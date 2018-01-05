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


import java.util.Date;
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
     * Creates a new ModeledActivityRecord backed by the given model object.
     * Changes to this record will affect the backing model object, and changes
     * to the backing model object will affect this record.
     * 
     * @param model
     *     The model object to use to back this activity record.
     */
    public ModeledActivityRecord(ActivityRecordModel model) {
        this.model = model;
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

}
