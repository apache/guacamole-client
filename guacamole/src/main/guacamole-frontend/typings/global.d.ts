import * as GuacTypes from 'guacamole-common-js';

/**
 * Augment the global scope with the Guacamole types.
 */
declare global {
    const Guacamole: typeof GuacTypes;
}
