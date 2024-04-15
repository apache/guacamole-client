import { Connection } from '../../rest/types/Connection';
import { DirectoryPatch } from '../../rest/types/DirectoryPatch';
import { ParseError } from './ParseError';

/**
 * The result of parsing a connection import file - containing a list of
 * API patches ready to be submitted to the PATCH REST API for batch
 * connection creation/replacement, a set of users and user groups to grant
 * access to each connection, a group path for every connection, and any
 * errors that may have occurred while parsing each connection.
 */
export class ParseResult {

    /**
     * An array of patches, ready to be submitted to the PATCH REST API for
     * batch connection creation / replacement. Note that this array may
     * contain more patches than connections from the original file - in the
     * case that connections are being fully replaced, there will be a
     * remove and a create patch for each replaced connection.
     */
    patches: DirectoryPatch<Connection>[];

    /**
     * An object whose keys are the user identifiers of users specified
     * in the batch import, and whose values are an array of indices of
     * connections within the patches array to which those users should be
     * granted access.
     */
    users: Record<string, number[]>;

    /**
     * An object whose keys are the user group identifiers of every user
     * group specified in the batch import. i.e. a set of all user group
     * identifiers.
     */
    groups: Record<string, number[]>;

    /**
     * A map of connection index within the patch array, to connection group
     * path for that connection, of the form "ROOT/Parent/Child".
     */
    groupPaths: Record<string, string>;

    /**
     * An array of errors encountered while parsing the corresponding
     * connection (at the same array index in the patches array). Each
     * connection should have an array of errors. If empty, no errors
     * occurred for this connection.
     */
    errors: ParseError[][];

    /**
     * True if any errors were encountered while parsing the connections
     * represented by this ParseResult. This should always be true if there
     * are a non-zero number of elements in the errors list for any
     * connection, or false otherwise.
     */
    hasErrors: boolean;

    /**
     * The integer number of unique connections present in the parse result.
     * This may be less than the length of the patches array, if any REMOVE
     * patches are present.
     */
    connectionCount: number;

    /**
     * Creates a new ParseResult.
     */
    constructor(template: Partial<ParseResult> = {}) {

        this.patches = template.patches || [];
        this.users = template.users || {};
        this.groups = template.groups || {};
        this.groupPaths = template.groupPaths || {};
        this.errors = template.errors || [];
        this.hasErrors = template.hasErrors || false;
        this.connectionCount = template.connectionCount || 0;

    }
}
