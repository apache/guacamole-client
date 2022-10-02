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

package org.apache.guacamole.event;

import javax.annotation.Nonnull;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.net.event.ApplicationShutdownEvent;
import org.apache.guacamole.net.event.ApplicationStartedEvent;
import org.apache.guacamole.net.event.DirectoryEvent;
import org.apache.guacamole.net.event.DirectoryFailureEvent;
import org.apache.guacamole.net.event.DirectorySuccessEvent;
import org.apache.guacamole.net.event.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that records each event that occurs in the logs, such as changes
 * made to objects via the REST API.
 */
public class EventLoggingListener implements Listener {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(EventLoggingListener.class);

    /**
     * Logs that an operation was performed on an object within a Directory
     * successfully.
     *
     * @param event
     *     The event describing the operation successfully performed on the
     *     object.
     */
    private void logSuccess(DirectorySuccessEvent<?> event) {
        DirectoryEvent.Operation op = event.getOperation();
        switch (op) {

            case GET:
                logger.debug("{} successfully accessed/retrieved {}", new RequestingUser(event), new AffectedObject(event));
                break;

            case ADD:
                logger.info("{} successfully created {}", new RequestingUser(event), new AffectedObject(event));
                break;

            case UPDATE:
                logger.info("{} successfully updated {}", new RequestingUser(event), new AffectedObject(event));
                break;

            case REMOVE:
                logger.info("{} successfully deleted {}", new RequestingUser(event), new AffectedObject(event));
                break;

            default:
                logger.warn("DirectoryEvent operation type has no corresponding log message implemented: {}", op);
                logger.info("{} successfully performed an unknown action on {} {}", new RequestingUser(event), new AffectedObject(event));

        }
    }

    /**
     * Logs that an operation failed to be performed on an object within a
     * Directory.
     *
     * @param event
     *     The event describing the operation that failed.
     */
    private void logFailure(DirectoryFailureEvent<?> event) {
        DirectoryEvent.Operation op = event.getOperation();
        switch (op) {

            case GET:
                if (event.getFailure() instanceof GuacamoleResourceNotFoundException)
                    logger.debug("{} failed to access/retrieve {}: {}", new RequestingUser(event), new AffectedObject(event), new Failure(event));
                else
                    logger.info("{} failed to access/retrieve {}: {}", new RequestingUser(event), new AffectedObject(event), new Failure(event));
                break;

            case ADD:
                logger.info("{} failed to create {}: {}", new RequestingUser(event), new AffectedObject(event), new Failure(event));
                break;

            case UPDATE:
                logger.info("{} failed to update {}: {}", new RequestingUser(event), new AffectedObject(event), new Failure(event));
                break;

            case REMOVE:
                logger.info("{} failed to delete {}: {}", new RequestingUser(event), new AffectedObject(event), new Failure(event));
                break;

            default:
                logger.warn("DirectoryEvent operation type has no corresponding log message implemented: {}", op);
                logger.info("{} failed to perform an unknown action on {}: {}", new RequestingUser(event), new AffectedObject(event), new Failure(event));

        }
    }

    @Override
    public void handleEvent(@Nonnull Object event) throws GuacamoleException {

        // General object creation/modification/deletion
        if (event instanceof DirectorySuccessEvent)
            logSuccess((DirectorySuccessEvent<?>) event);
        else if (event instanceof DirectoryFailureEvent)
            logFailure((DirectoryFailureEvent<?>) event);

        // Application startup/shutdown
        else if (event instanceof ApplicationStartedEvent)
            logger.info("The Apache Guacamole web application has started.");
        else if (event instanceof ApplicationShutdownEvent)
            logger.info("The Apache Guacamole web application has shut down.");

        // Unknown events
        else
            logger.debug("Ignoring unknown/unimplemented event type: {}",
                    event.getClass());

    }

}
