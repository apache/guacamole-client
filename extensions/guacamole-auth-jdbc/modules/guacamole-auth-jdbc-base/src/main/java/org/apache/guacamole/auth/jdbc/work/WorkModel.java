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

package org.apache.guacamole.auth.jdbc.work;

import java.util.Date;

import org.apache.guacamole.auth.jdbc.base.ChildObjectModel;

public class WorkModel extends ChildObjectModel {
    /**
     * The human-readable name associated with this work.
     */
    private String name;

    /**
     * Date to start work.
     */
    private Date startDate;

    /**
     * Date to end work.
     */
    private Date endDate;

    /**
     * Creates a new, empty WorkModel.
     */
    public WorkModel() {
    }

    /**
     * Returns the name associated with this work.
     *
     * @return
     *     The name associated with this work.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name associated with this work.
     *
     * @param name
     *     The name to associate with this work.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the date to start work.
     *
     * @return
     *     The date to start work.
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * Sets the date to start work.
     *
     * @param date
     *     The date to start work.
     */
    public void setStartDate(Date date) {
        this.startDate = date;
    }

    /**
     * Returns the date to end work.
     *
     * @return
     *     The date to end work.
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * Sets the date to end work.
     *
     * @param date
     *     The date to end work.
     */
    public void setEndDate(Date date) {
        this.endDate = date;
    }

}