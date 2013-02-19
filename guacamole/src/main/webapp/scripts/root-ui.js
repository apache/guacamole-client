
/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * General set of UI elements and UI-related functions regarding user login and
 * connection management.
 */
var GuacamoleRootUI = {

    "sections": {
        "login_form"         : document.getElementById("login-form"),
        "recent_connections" : document.getElementById("recent-connections"),
        "all_connections"    : document.getElementById("all-connections"),
        "all_connections_buttons" : document.getElementById("all-connections-buttons")
    },

    "messages": {
        "login_error"           : document.getElementById("login-error"),
        "no_recent_connections" : document.getElementById("no-recent")
    },

    "fields": {
        "username"  : document.getElementById("username"),
        "password"  : document.getElementById("password"),
        "clipboard" : document.getElementById("clipboard")
    },
    
    "buttons": {
        "login"  : document.getElementById("login"),
        "logout" : document.getElementById("logout"),
        "manage" : document.getElementById("manage")
    },

    "settings": {
        "auto_fit"      : document.getElementById("auto-fit"),
        "disable_sound" : document.getElementById("disable-sound")
    },

    "views": {
        "login"       : document.getElementById("login-ui"),
        "connections" : document.getElementById("connection-list-ui")
    },

    "session_state" :  new GuacamoleSessionState()

};

/**
 * Attempts to login the given user using the given password, throwing an
 * error if the process fails.
 * 
 * @param {String} username The name of the user to login as.
 * @param {String} password The password to use to authenticate the user.
 */
GuacamoleRootUI.login = function(username, password) {

    // Get parameters from query string
    var parameters = window.location.search.substring(1);

    // Get username and password from form
    var data =
           "username=" + encodeURIComponent(username)
        + "&password=" + encodeURIComponent(password)

    // Include query parameters in submission data
    if (parameters) data += "&" + parameters;

    // Log in
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "login", false);
    xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhr.send(data);

    // Handle failures
    if (xhr.status != 200)
        throw new Error("Invalid login");

};

/**
 * Set of all thumbnailed connections, indexed by ID.
 */
GuacamoleRootUI.thumbnailConnections = {};

/**
 * Set of all connections, indexed by ID.
 */
GuacamoleRootUI.connections = {};

/**
 * Adds the given connection to the recent connections list.
 */
GuacamoleRootUI.addRecentConnection = function(connection) {

    // Add connection object to list of thumbnailed connections
    GuacamoleRootUI.thumbnailConnections[connection.connection.id] =
        connection;
    
    // Add connection to recent list
    GuacamoleRootUI.sections.recent_connections.appendChild(
        connection.getElement());

    // Hide "No recent connections" message
    GuacamoleRootUI.messages.no_recent_connections.style.display = "none";

};


/**
 * Resets the interface such that the login UI is displayed if
 * the user is not authenticated (or authentication fails) and
 * the connection list UI (or the client for the only available
 * connection, if there is only one) is displayed if the user is
 * authenticated.
 */
GuacamoleRootUI.reset = function() {

    // Get parameters from query string
    var parameters = window.location.search.substring(1);

    function hasEntry(object) {
        for (var name in object)
            return true;
        return false;
    }

    // Read connections
    var connections;
    try {
        connections = GuacamoleService.Connections.list(parameters);

        // Sort connections by ID
        connections.sort(GuacamoleService.Connections.comparator);

        // Show admin elements if admin permissions available
        var permissions = GuacamoleService.Permissions.list();
        if (permissions.create_connection
            || permissions.create_user
            || hasEntry(permissions.update_user)
            || hasEntry(permissions.remove_user)
            || hasEntry(permissions.administer_user)
            || hasEntry(permissions.update_connection)
            || hasEntry(permissions.remove_connection)
            || hasEntry(permissions.administer_connection))
                GuacUI.addClass(document.body, "admin");
            else
                GuacUI.removeClass(document.body, "admin");

    }
    catch (e) {

        // Show login UI if unable to get connections
        GuacamoleRootUI.views.login.style.display = "";
        GuacamoleRootUI.views.connections.style.display = "none";

        return;

    }

    // Create pager for connections
    var connection_pager = new GuacUI.Pager(GuacamoleRootUI.sections.all_connections);
    connection_pager.page_capacity = 20;

    // Add connection icons
    for (var i=0; i<connections.length; i++) {

        // Add connection to set
        GuacamoleRootUI.connections[connections[i].id] = connections[i];

        // Get connection element
        var connection = new GuacUI.Connection(connections[i]);

        // If screenshot present, add to recent connections
        if (connection.hasThumbnail())
            GuacamoleRootUI.addRecentConnection(connection);

        // Add connection to connection list
        connection_pager.addElement(
            new GuacUI.Connection(connections[i]).getElement());

    }

    // Add buttons if more than one page
    if (connection_pager.last_page != 0)
        GuacamoleRootUI.sections.all_connections_buttons.appendChild(
            connection_pager.getElement());

    // Start at page 0
    connection_pager.setPage(0);

    // If connections could be retrieved, display list
    GuacamoleRootUI.views.login.style.display = "none";
    GuacamoleRootUI.views.connections.style.display = "";

};

GuacamoleHistory.onchange = function(id, old_entry, new_entry) {

    // Get existing connection, if any
    var connection = GuacamoleRootUI.thumbnailConnections[id];

    // If we are adding or updating a connection
    if (new_entry) {

        // Ensure connection is added
        if (!connection) {

            // If connection not actually defined, storage must be being
            // modified externally. Stop early.
            if (!GuacamoleRootUI.connections[id]) return;

            // Create new connection
            connection = new GuacUI.Connection(
                GuacamoleRootUI.connections[id]
            );

            GuacamoleRootUI.addRecentConnection(connection);

        }

        // Set new thumbnail 
        connection.setThumbnail(new_entry.thumbnail);

    }

    // Otherwise, delete existing connection
    else {

        GuacamoleRootUI.sections.recent_connections.removeChild(
            connection.getElement());

        delete GuacamoleRootUI.thumbnailConnections[id];

        // Display "No recent connections" message if none left
        if (GuacamoleRootUI.thumbnailConnections.length == 0)
            GuacamoleRootUI.messages.no_recent_connections.style.display = "";

    }
    
};

/*
 * This window has no name. We need it to have no name. If someone navigates
 * to the root UI within the same window as a previous connection, we need to
 * remove the name from that window such that new attempts to use that previous
 * connection do not replace the contents of this very window.
 */
window.name = "";

/*
 * Update session state when auto-fit checkbox is changed
 */

GuacamoleRootUI.settings.auto_fit.onchange =
GuacamoleRootUI.settings.auto_fit.onclick  = function() {

    GuacamoleRootUI.session_state.setProperty(
        "auto-fit", GuacamoleRootUI.settings.auto_fit.checked);

};

/*
 * Update session state when disable-sound checkbox is changed
 */

GuacamoleRootUI.settings.disable_sound.onchange =
GuacamoleRootUI.settings.disable_sound.onclick  = function() {

    GuacamoleRootUI.session_state.setProperty(
        "disable-sound", GuacamoleRootUI.settings.disable_sound.checked);

};

/*
 * Update clipboard contents when changed
 */

window.onblur =
GuacamoleRootUI.fields.clipboard.onchange = function() {

    // Set value if changed
    var new_value = GuacamoleRootUI.fields.clipboard.value;
    if (GuacamoleRootUI.session_state.getProperty("clipboard") != new_value)
        GuacamoleRootUI.session_state.setProperty("clipboard", new_value);

};

/*
 * Update element states when session state changes
 */

GuacamoleRootUI.session_state.onchange =
function(old_state, new_state, name) {

    // Clipboard
    if (name == "clipboard")
        GuacamoleRootUI.fields.clipboard.value = new_state[name];

    // Auto-fit display
    else if (name == "auto-fit")
        GuacamoleRootUI.fields.auto_fit.checked = new_state[name];

    // Disable Sound
    else if (name == "disable-sound")
        GuacamoleRootUI.fields.disable_sound.checked = new_state[name];

};

/*
 * Initialize clipboard with current data
 */

if (GuacamoleRootUI.session_state.getProperty("clipboard"))
    GuacamoleRootUI.fields.clipboard.value =
        GuacamoleRootUI.session_state.getProperty("clipboard");

/*
 * Default to true if auto-fit not specified
 */

if (GuacamoleRootUI.session_state.getProperty("auto-fit") === undefined)
    GuacamoleRootUI.session_state.setProperty("auto-fit", true);

/*
 * Initialize auto-fit setting in UI
 */

GuacamoleRootUI.settings.auto_fit.checked =
    GuacamoleRootUI.session_state.getProperty("auto-fit");

/*
 * Initialize disable-sound setting in UI
 */
GuacamoleRootUI.settings.disable_sound.checked =
    GuacamoleRootUI.session_state.getProperty("disable-sound");

/*
 * Set handler for logout
 */

GuacamoleRootUI.buttons.logout.onclick = function() {
    window.location.href = "logout";
};

/*
 * Set handler for admin
 */

GuacamoleRootUI.buttons.manage.onclick = function() {
    window.location.href = "admin.xhtml";
};

/*
 * Set handler for login
 */

GuacamoleRootUI.sections.login_form.onsubmit = function() {

    try {

        // Attempt login
        GuacamoleRootUI.login(
            GuacamoleRootUI.fields.username.value,
            GuacamoleRootUI.fields.password.value
        );

        // Ensure username/password fields are blurred after login attempt
        GuacamoleRootUI.fields.username.blur();
        GuacamoleRootUI.fields.password.blur();

        // Reset UI
        GuacamoleRootUI.reset();

    }
    catch (e) {

        // Display error, reset and refocus password field
        GuacamoleRootUI.messages.login_error.textContent = e.message;

        // Reset and recofus password field
        GuacamoleRootUI.fields.password.value = "";
        GuacamoleRootUI.fields.password.focus();

    }

    // Always cancel submit
    return false;

};

/*
 * Turn off autocorrect and autocapitalization on usename 
 */

GuacamoleRootUI.fields.username.setAttribute("autocorrect", "off");
GuacamoleRootUI.fields.username.setAttribute("autocapitalize", "off");

/*
 * Initialize UI
 */

GuacamoleRootUI.reset();

/*
 * Make sure body has an associated touch event handler such that CSS styles
 * will work in browsers that require this.
 */
document.body.ontouchstart = function() {};
