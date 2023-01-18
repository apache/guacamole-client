package org.apache.guacamole.rest.directory;

import org.apache.guacamole.GuacamoleException;

public class DirectoryOperationException<InternalType> extends GuacamoleException {

    public DirectoryOperationException(String message) {
        super(message);
    }


}
