
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

function GuacamoleMessage(xml) {

    var server = null;
    var framebuffer = null;
    var errors = new Array();
    var updates = new Array();


    this.getServer = function() { return server; };
    this.getFramebuffer = function() { return framebuffer; };
    this.getErrors = function() { return errors; };
    this.hasErrors = function() { return errors.length > 0; };
    this.getUpdates = function() { return updates; };

    if (xml) {

        // Parse document
        var root = xml.documentElement;
        if (root) {

            // Parse errors
            var errorElements = root.getElementsByTagName("error");
            if (errorElements.length >= 1) {
                for (var errorIndex=0; errorIndex<errorElements.length; errorIndex++)
                    errors.push(new GuacamoleError(errorElements[errorIndex].textContent));
            }

            // Parse server element
            var servers = root.getElementsByTagName("server");
            if (servers.length == 1)
                server = new Server(servers[0].attributes['name'].value);

            // Parse framebuffer element
            var framebuffers = root.getElementsByTagName("framebuffer");
            if (framebuffers.length == 1) {
                var width = framebuffers[0].attributes['width'].value;
                var height = framebuffers[0].attributes['height'].value;
                framebuffer = new Framebuffer(width, height);
            }

            // Parse clipboard update elements
            var clipboards = root.getElementsByTagName("clipboard");
            if (clipboards.length == 1) {
                var data = clipboards[0].textContent;
                updates.push(new ClipboardUpdate(data));
            }

            // Parse framebuffer update elements 
            var updateElements = root.getElementsByTagName("update");
            for (var i=0; i<updateElements.length; i++) {

                var update = updateElements[i];
                var type = update.attributes['type'].value;
                var x = parseInt(update.attributes['x'].value);
                var y = parseInt(update.attributes['y'].value);
                var index = parseInt(update.attributes['index'].value);

                var image_uuid;
                var url;

                // Image type update
                if (type == "image") {

                    // Use in-band image if given, otherwise use out-of-band
                    if (update.textContent)
                        url = "data:image/png;base64," + update.textContent;
                    else {
                        image_uuid = update.attributes['image'].value;
                        url = "stream.png?image=" + image_uuid;
                    }

                    updates.push(new FramebufferUpdate(index, x, y, url));
                } // end if image update

                // CopyRect
                else if (type == "copyrect") {

                    var srcx = update.attributes['srcx'].value;
                    var srcy = update.attributes['srcy'].value;
                    var width = update.attributes['width'].value;
                    var height = update.attributes['height'].value;

                    updates.push(new CopyRectFramebufferUpdate(index, srcx, srcy, width, height, x, y));
                } // end if copyrect

                // Cursor
                else if (type == "cursor") {

                    // Use in-band image if given, otherwise use out-of-band
                    if (update.textContent)
                        url = "data:image/png;base64," + update.textContent;
                    else {
                        image_uuid = update.attributes['image'].value;
                        url = "stream.png?image=" + image_uuid;
                    }

                    updates.push(new CursorFramebufferUpdate(index, x, y, url));
                }

            } // end for all updates

        } // end if root

    } // end if xml

}

function Server(name, width, height) {
    this.getName   = function() { return name; };
}

function Framebuffer(width, height) {

    this.getWidth  = function() { return width; };
    this.getHeight = function() { return height; };

}

function GuacamoleError(message) {
    this.getMessage = function() { return message; };
}

function ClipboardUpdate(data) {
    this.getData = function() { return data; };
}

function FramebufferUpdate(index, x, y, url) {

    this.getIndex = function() { return index; };
    this.getX = function() { return x; };
    this.getY = function() { return y; };
    this.getURL = function() { return url; };

}

function CursorFramebufferUpdate(index, hotspotX, hotspotY, url) {

    this.getIndex = function() { return index; };
    this.getHotspotX = function() { return hotspotX; };
    this.getHotspotY = function() { return hotspotY; };
    this.getURL = function() { return url; };

}

function CopyRectFramebufferUpdate(index, srcx, srcy, width, height, dstx, dsty) {

    this.getIndex = function() { return index; };
    this.getSrcX = function() { return srcx; };
    this.getSrcY = function() { return srcy; };
    this.getWidth  = function() { return width; };
    this.getHeight = function() { return height; };
    this.getDstX = function() { return dstx; };
    this.getDstY = function() { return dsty; };

}

