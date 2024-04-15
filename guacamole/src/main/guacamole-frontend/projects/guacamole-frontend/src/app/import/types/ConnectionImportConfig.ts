/**
 * A representation of any user-specified configuration when
 * batch-importing connections.
 */
export class ConnectionImportConfig {

    /**
     * The mode for handling connections that match existing connections.
     */
    existingConnectionMode: ConnectionImportConfig.ExistingConnectionMode;

    /**
     * The mode for handling permissions on existing connections that are
     * being updated. Only meaningful if the importer is configured to
     * replace existing connections.
     */
    existingPermissionMode: ConnectionImportConfig.ExistingPermissionMode;

    /**
     * Creates a new ConnectionImportConfig.
     *
     * @param [template={}]
     *     The object whose properties should be copied within the new
     *     ConnectionImportConfig.
     */
    constructor(template: Partial<ConnectionImportConfig> = {}) {

        this.existingConnectionMode = template.existingConnectionMode
            || ConnectionImportConfig.ExistingConnectionMode.REJECT;

        this.existingPermissionMode = template.existingPermissionMode
            || ConnectionImportConfig.ExistingPermissionMode.PRESERVE;

    }

}

export namespace ConnectionImportConfig {

    /**
     * Valid modes for the behavior of the importer when an imported connection
     * already exists.
     */
    export enum ExistingConnectionMode {

        /**
         * Any Connection that has the same name and parent group as an existing
         * connection will cause the entire import to be rejected with an error.
         */
        REJECT = 'REJECT',

        /**
         * Replace/update any existing connections.
         */
        REPLACE = 'REPLACE'

    }

    /**
     * Valid modes for the behavior of the importer with respect to connection
     * permissions when existing connections are being replaced.
     */
    export enum ExistingPermissionMode {

        /**
         * Any new permissions specified in the imported connection will be
         * added to the existing connection, without removing any existing
         * permissions.
         */
        PRESERVE = 'PRESERVE',

        /**
         * Any existing permissions will be removed, ensuring that only the
         * users or groups specified in the import file will be granted to the
         * replaced connection after import.
         */
        REPLACE = 'REPLACE'

    }


}
