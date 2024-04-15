/**
 * A collection of connection-group-tree-derived maps that are useful for
 * processing connections.
 */
export class TreeLookups {

    /**
     * A map of all known group paths to the corresponding identifier for
     * that group. The is that a user-provided import file might directly
     * specify a named group path like "ROOT", "ROOT/parent", or
     * "ROOT/parent/child". This field field will map all of the above to
     * the identifier of the appropriate group, if defined.
     */
    groupPathsByIdentifier: Record<string, string>;

    /**
     * A map of all known group identifiers to the path of the corresponding
     * group. These paths are all of the form "ROOT/parent/child".
     */
    groupIdentifiersByPath: Record<string, string>;

    /**
     * A map of group identifier, to connection name, to connection
     * identifier. These paths are all of the form "ROOT/parent/child". The
     * idea is that existing connections can be found by checking if a
     * connection already exists with the same parent group, and with the
     * same name as an user-supplied import connection.
     */
    connectionIdsByGroupAndName: Record<string, string>;

    /**
     * Creates a new TreeLookups object.
     *
     * @param  template
     *     The object whose properties should be copied within the new
     *     ConnectionImportConfig.
     */
    constructor(template: Partial<TreeLookups>) {

        this.groupPathsByIdentifier = template.groupPathsByIdentifier || {};
        this.groupIdentifiersByPath = template.groupIdentifiersByPath || {};
        this.connectionIdsByGroupAndName = template.connectionIdsByGroupAndName || {};

    }

}
