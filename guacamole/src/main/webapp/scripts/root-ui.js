
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
        "other_connections"  : document.getElementById("other-connections")
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
        "logout" : document.getElementById("logout")
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
 * Set of thumbnails for each connection, indexed by ID.
 */
GuacamoleRootUI.Thumbnails = new (function() {

    var thumbnails =
        JSON.parse(localStorage.getItem("GUAC_THUMBNAILS") || "{}");

    /**
     * Returns the URL for the thumbnail of the connection with the given ID,
     * or undefined if no thumbnail is associated with that connection.
     */
    this.getURL = function(id) {
        return thumbnails[id];
    };

});

/**
 * A connection UI object which can be easily added to a list of connections
 * for sake of display.
 */
GuacamoleRootUI.Connection = function(id, protocol_name) {

    function element(tagname, classname) {
        var new_element = document.createElement(tagname);
        new_element.className = classname;
        return new_element;
    }

    // Create connection display elements
    var connection    = element("div",  "connection");
    var caption       = element("div",  "caption");
    var protocol      = element("div",  "protocol");
    var name          = element("span", "name");
    var protocol_icon = element("div",  "icon " + protocol_name);
    var thumbnail     = element("div",  "thumbnail");

    // Get URL
    var url = "client.xhtml?id=" + encodeURIComponent(id);

    // Create link to client
    connection.onclick = function() {

        // Attempt to focus existing window
        var current = window.open(null, id);

        // If window did not already exist, set up as
        // Guacamole client
        if (!current.GuacamoleUI)
            window.open(url, id);

    };

    // Add icon
    protocol.appendChild(protocol_icon);

    // Set name
    name.textContent = id;

    // Assemble caption
    caption.appendChild(protocol);
    caption.appendChild(name);

    // Assemble connection icon
    connection.appendChild(thumbnail);
    connection.appendChild(caption);

    // Add screenshot if available
    var thumbnail_url = GuacamoleRootUI.Thumbnails.getURL(id);
    if (thumbnail_url) {

        // Create thumbnail element
        var img = document.createElement("img");
        img.src = thumbnail_url;
        thumbnail.appendChild(img);

    }

    /**
     * Returns the DOM element representing this connection.
     */
    this.getElement = function() {
        return connection;
    };

    /**
     * Returns whether this connection has an associated thumbnail.
     */
    this.hasThumbnail = function() {
        return thumbnail_url && true;
    };

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

    // Read configs
    var configs;
    try {
        configs = getConfigList(parameters);
    }
    catch (e) {

        // Show login UI if unable to get configs
        GuacamoleRootUI.views.login.style.display = "";
        GuacamoleRootUI.views.connections.style.display = "none";

        return;

    }

    // Add connection icons
    for (var i=0; i<configs.length; i++) {

        // Create connection element
        var connection = new GuacamoleRootUI.Connection(
            configs[i].id, configs[i].protocol);

        // If screenshot presennt, add to recent connections
        if (connection.hasThumbnail()) {

            // Add connection to recent list
            GuacamoleRootUI.sections.recent_connections.appendChild(
                connection.getElement());

            // Hide "No recent connections" message
            GuacamoleRootUI.messages.no_recent_connections.style.display =
                "none";

        }

        // Otherwise, add conection to other connection list
        else
            GuacamoleRootUI.sections.other_connections.appendChild(
                connection.getElement());

    }

    // If configs could be retrieved, display list
    GuacamoleRootUI.views.login.style.display = "none";
    GuacamoleRootUI.views.connections.style.display = "";

};

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

GuacamoleRootUI.fields.clipboard.onchange = function() {

    GuacamoleRootUI.session_state.setProperty(
        "clipboard", GuacamoleRootUI.fields.clipboard.value);

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
