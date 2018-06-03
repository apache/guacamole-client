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
package org.apache.guacamole.auth.quickconnect;

import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.language.Translatable;
import org.apache.guacamole.language.TranslatableMessage;

/**
 * An exception that is thrown by this extension when an error occurs
 * attempting to create and establish a connection with a user-provided
 * URI.
 */
public class QuickConnectException extends GuacamoleClientException
        implements Translatable {
    
    /**
     * A message that can be passed through the translation service
     * to provide information about the error that occurred.
     */
    private final TranslatableMessage translatableMessage;
    
    /**
     * Create a QuickConnectException with the given message and translationKey.
     * The message will not be passed through the translation system; the
     * translationKey will be passed through the translation system.  Both should
     * describe the error.
     * 
     * @param message
     *     A string describing the error that occurred when trying to create
     *     or establish the connection.  This will not be passed through the
     *     translation system.
     * 
     * @param translationKey
     *     A key known to the translation system describing the error that 
     *     occurred when trying to create or establish the connection.
     *     This will be passed through the translation system to provide 
     *     a localized version of the message.
     */
    public QuickConnectException(String message, String translationKey) {
        super(message);
        this.translatableMessage = new TranslatableMessage(translationKey);
    }
    
    /**
     * Create a new QuickConnectException given the human-readable message,
     * which will not be passed through the translation system, and the
     * translatableMessage, which will be passed through the translation system.
     * Both parameters should describe the error preventing the connection
     * from being created or established.
     * 
     * @param message
     *     The human-readable message describing the error, which will not
     *     be passed through the translation system.
     * 
     * @param translatableMessage
     *     The human-readable message describing the error, which will be
     *     passed through the translation system.
     */
    public QuickConnectException(String message, TranslatableMessage translatableMessage) {
        super(message);
        this.translatableMessage = translatableMessage;
    }
    
    @Override
    public TranslatableMessage getTranslatableMessage() {
        return translatableMessage;
    }
    
}
