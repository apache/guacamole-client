import { ParseError } from './ParseError';

/**
 * All configuration required to generate a function that can
 * transform a row of CSV into a connection object.
 */
export interface TransformConfig {

    // Callbacks for required fields
    nameGetter?: (row: string[]) => any;
    protocolGetter?: (row: string[]) => any;

    // Callbacks for a parent group ID or group path
    groupGetter?: (row: string[]) => any;
    parentIdentifierGetter?: (row: string[]) => any;

    // Callbacks for user and user group identifiers
    usersGetter: (row: string[]) => string[];
    userGroupsGetter: (row: string[]) => string[];

    // Callbacks that will generate either connection attributes or
    // parameters. These callbacks will return a {type, name, value}
    // object containing the type ("parameter" or "attribute"),
    // the name of the attribute or parameter, and the corresponding
    // value.
    parameterOrAttributeGetters: ((row: string[]) => {
        type?: string;
        name?: string;
        value?: any;
        errors?: ParseError[]
    })[];

}
