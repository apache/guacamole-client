import { ParseError } from './ParseError';

/**
 * A list of human-readable error messages, intended to be usable in a
 * sortable / filterable table.
 */
export class DisplayErrorList {

    /**
     * The error messages that should be prepared for display.
     */
    messages: (string | ParseError)[];

    /**
     * The single String message composed of all messages concatenated
     * together. This will be used for filtering / sorting, and should only
     * be calculated once, when toString() is called.
     */
    concatenatedMessage: string | null;

    /**
     * Creates a new DisplayErrorList.
     *
     * @param messages
     *     The error messages that should be prepared for display.
     */
    constructor(messages?: (string | ParseError)[]) {

        this.messages = messages || [];
        this.concatenatedMessage = null;

    }

    /**
     * Return a sortable / filterable representation of all the error messages
     * wrapped by this DisplayErrorList.
     *
     * NOTE: Once this method is called, any changes to the underlying array
     * will have no effect. This is to ensure that repeated calls to toString()
     * by sorting / filtering UI code will not regenerate the concatenated
     * message every time.
     *
     * @returns
     *     A sortable / filterable representation of the error messages wrapped
     *     by this DisplayErrorList
     */
    toString(): string {

        // Generate the concatenated message if not already generated
        if (!this.concatenatedMessage)
            this.concatenatedMessage = this.messages.join(' ');

        return this.concatenatedMessage;

    }

    /**
     * Return the underlying array containing the raw error messages, wrapped
     * by this DisplayErrorList.
     *
     * @returns
     *     The underlying array containing the raw error messages, wrapped by
     *     this DisplayErrorList
     */
    getArray(): (string | ParseError)[] {
        return this.messages;
    }

}
