import { Optional } from '../../util/utility-types';
import { ParseError } from './ParseError';

/**
 * A representation of a connection to be imported, as parsed from an
 * user-supplied import file.
 */
export class ImportConnection {

    /**
     * The unique identifier of the connection group that contains this
     * connection.
     */
    parentIdentifier: string;

    /**
     * The path to the connection group that contains this connection,
     * written as e.g. "ROOT/parent/child/group".
     */
    group: string;

    /**
     * The identifier of the connection being updated. Only meaningful if
     * the replace operation is set.
     */
    identifier: string;

    /**
     * The human-readable name of this connection, which is not necessarily
     * unique.
     */
    name: string;

    /**
     * The name of the protocol associated with this connection, such as
     * "vnc" or "rdp".
     */
    protocol: string;

    /**
     * Connection configuration parameters, as dictated by the protocol in
     * use, arranged as name/value pairs.
     */
    parameters: Record<string, string>;

    /**
     * Arbitrary name/value pairs which further describe this connection.
     * The semantics and validity of these attributes are dictated by the
     * extension which defines them.
     */
    attributes: Record<string, string>;

    /**
     * The identifiers of all users who should be granted read access to
     * this connection.
     */
    users: string[];

    /**
     * The identifiers of all user groups who should be granted read access
     * to this connection.
     */
    groups: string[];

    /**
     * The mode import mode for this connection. If not otherwise specified,
     * a brand new connection should be created.
     */
    importMode: ImportConnection.ImportMode;

    /**
     * Any errors specific to this connection encountered while parsing.
     */
    errors: ParseError[];

    /**
     * Create a new ImportConnection.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     Connection.
     */
    constructor(template: Optional<ImportConnection, 'parameters' |
        'attributes' | 'users' | 'groups' | 'importMode' | 'errors'>) {

        this.parentIdentifier = template.parentIdentifier;
        this.group = template.group;
        this.identifier = template.identifier;
        this.name = template.name;
        this.protocol = template.protocol;
        this.parameters = template.parameters || {};
        this.attributes = template.attributes || {};
        this.users = template.users || [];
        this.groups = template.groups || [];
        this.importMode = template.importMode || ImportConnection.ImportMode.CREATE;
        this.errors = template.errors || [];
    }

}

export namespace ImportConnection {

    /**
     * The possible import modes for a given connection.
     */
    export enum ImportMode {

        /**
         * The connection should be created fresh. This mode is valid IFF there
         * is no existing connection with the same name and parent group.
         */
        CREATE = 'CREATE',

        /**
         * This connection will replace the existing connection with the same
         * name and parent group.
         */
        REPLACE = 'REPLACE'

    }

}
