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

package org.apache.guacamole.extension;

/**
 * Provider of general, human-readable text that describes the context of a
 * logger. This is currently used to allow our various custom class loaders to
 * provide contextual information, which our custom logger factory can pull from
 * the call stack when loggers are created.
 */
public interface LoggerContextProvider {

    /**
     * Returns human-readable text describing the context of events logged by
     * a logger. The text should describe the general context of the source of
     * the entry.
     *
     * @return
     *     Human-readable text describing the context of events logged by any
     *     logger created/maintained by the class implementing this interface.
     */
    String getLoggerContext();

}
