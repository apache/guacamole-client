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
package org.apache.guacamole.auth.restrict;

import org.apache.guacamole.calendar.RestrictionType;
import org.apache.guacamole.net.auth.Attributes;

/**
 * An interface which defines methods that apply to items that can have
 * restrictions applied to them.
 */
public interface Restrictable extends Attributes {
    
    /**
     * The name of the attribute that contains the absolute date and time after
     * which this restrictable object may be used. If this attribute is present
     * access to to this object will be denied at any time prior to the parsed
     * value of this attribute, regardless of what other restrictions may be
     * present to allow access to the object at certain days/times of the week
     * or from certain hosts.
     */
    public static final String RESTRICT_TIME_AFTER_ATTRIBUTE_NAME = "guac-restrict-time-after";
    
    /**
     * The name of the attribute that contains a list of weekdays and times (UTC)
     * that this restrictable object can be used. The presence of values within
     * this attribute will automatically restrict use of the object at any times
     * that are not specified.
     */
    public static final String RESTRICT_TIME_ALLOWED_ATTRIBUTE_NAME = "guac-restrict-time-allowed";
    
    /**
     * The name of the attribute that contains the absolute date and time before
     * which use of this restrictable object may be used. If this attribute is
     * present use of the object will be denied at any time after the parsed
     * value of this attribute, regardless of the presence of other restrictions
     * that may allow access at certain days/times of the week or from certain
     * hosts.
     */
    public static final String RESTRICT_TIME_BEFORE_ATTRIBUTE_NAME = "guac-restrict-time-before";
    
    /**
     * The name of the attribute that contains a list of weekdays and times (UTC)
     * that this restrictable object cannot be used. Denied times will always take
     * precedence over allowed times. The presence of this attribute without
     * guac-restrict-time-allowed will deny access only during the times listed
     * in this attribute, allowing access at all other times. The presence of
     * this attribute along with the guac-restrict-time-allowed attribute will
     * deny access at any times that overlap with the allowed times.
     */
    public static final String RESTRICT_TIME_DENIED_ATTRIBUTE_NAME = "guac-restrict-time-denied";
    
    /**
     * The name of the attribute that contains a list of hosts from which this
     * restrictable object may be used. The presence of this attribute will
     * restrict use to only users accessing Guacamole from the list of hosts
     * contained in the attribute, subject to further restriction by the
     * guac-restrict-hosts-denied attribute.
     */
    public static final String RESTRICT_HOSTS_ALLOWED_ATTRIBUTE_NAME = "guac-restrict-hosts-allowed";
    
    /**
     * The name of the attribute that contains a list of hosts from which this
     * restrictable object may not be used. The presence of this attribute,
     * absent the guac-restrict-hosts-allowed attribute, will allow use from
     * all hosts except the ones listed in this attribute. The presence of this
     * attribute coupled with the guac-restrict-hosts-allowed attribute will
     * block access from any IPs in this list, overriding any that may be
     * allowed.
     */
    public static final String RESTRICT_HOSTS_DENIED_ATTRIBUTE_NAME = "guac-restrict-hosts-denied";
    
    /**
     * Return the restriction state for this restrictable object at the
     * current date and time. By default returns an implicit denial.
     * 
     * @return 
     *     The restriction status for the current date and time.
     */
    default public RestrictionType getCurrentTimeRestriction() {
        return RestrictionType.IMPLICIT_DENY;
    }
    
    /**
     * Return the restriction state for this restrictable object for the host
     * from which the current user is logged in. By default returns an implicit
     * denial.
     * 
     * @return 
     *     The restriction status for the host from which the current user is
     *     logged in.
     */
    default public RestrictionType getCurrentHostRestriction() {
        return RestrictionType.IMPLICIT_DENY;
    }
    
    /**
     * Returns true if the current item is available based on the restrictions
     * for the given implementation of this interface, or false if the item is
     * not currently available. The default implementation checks current time
     * and host restrictions, allowing if both those restrictions allow access.
     * 
     * @return 
     *     true if the item is available, otherwise false.
     */
    default public boolean isAvailable() {
        return (getCurrentTimeRestriction().isAllowed() && getCurrentHostRestriction().isAllowed());
    }
    
}