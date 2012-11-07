/**
 * Set of thumbnails for each connection, indexed by ID.
 */
GuacamoleHistory = new (function() {

    var history =
        JSON.parse(localStorage.getItem("GUAC_HISTORY") || "{}");

    /**
     * Returns the URL for the thumbnail of the connection with the given ID,
     * or undefined if no thumbnail is associated with that connection.
     */
    this.get = function(id) {
        return history[id] || new GuacamoleHistory.Entry();
    };

    /**
     * Updates the thumbnail and access time of the history entry for the
     * connection with the given ID.
     */
    this.update = function(id, thumbnail) {

        // Create updated entry
        var entry = new GuacamoleHistory.Entry(
            id,
            thumbnail,
            new Date().getTime()
        );

        // Store entry in history
        history[id] = entry;

        // Save updated history
        localStorage.setItem("GUAC_HISTORY", JSON.stringify(history));

    };

})();

/**
 * A single entry in the indexed connection usage history.
 * 
 * @constructor
 * @param {String} id The ID of this connection.
 * @param {String} thumbnail The URL of the thumbnail to use to represent this
 *                           connection.
 * @param {Number} last_access The time this connection was last accessed, in
 *                             seconds.
 */
GuacamoleHistory.Entry = function(id, thumbnail, last_access) {

    /**
     * The ID of the connection associated with this history entry.
     */
    this.id = id;

    /**
     * The thumbnail associated with the connection associated with this history
     * entry.
     */
    this.thumbnail = thumbnail;

    /**
     * The time the connection associated with this entry was last accessed.
     */
    this.accessed = last_access;

};
 