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
package org.apache.guacamole.wol;

import org.apache.guacamole.GuacamoleException;

/**
 * An exception class for this extension that represents WOL-specific
 * errors that may occur.
 */
public class WOLException extends GuacamoleException {

    /**
     * Construct a WOLException object with the given message indicating
     * the nature of the exception.
     * 
     * @param message 
     *     The human-readable message that indicates the nature of the
     *     exception.
     */
    public WOLException(String message) {
        super(message);
    }

    /**
     * Construct a WOLException object with the given human-readable message
     * and the underlying cause for the exception being thrown.
     * 
     * @param message
     *     The human-readable message indicating the nature of the exception.
     * 
     * @param cause 
     *     The underlying cause of the exception.
     */
    public WOLException(String message, Throwable cause) {
        super(message, cause);
    }

}
