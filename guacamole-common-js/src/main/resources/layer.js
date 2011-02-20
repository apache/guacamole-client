
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

function Layer(width, height) {

    // Off-screen buffer
    var display = document.createElement("canvas");
    var displayContext = display.getContext("2d");

    function resize(newWidth, newHeight) {
        display.style.position = "absolute";
        display.style.left = "0px";
        display.style.top = "0px";

        display.width = newWidth;
        display.height = newHeight;

        width = newWidth;
        height = newHeight;
    }

    display.resize = function(newWidth, newHeight) {
        if (newWidth != width || newHeight != height)
            resize(newWidth, newHeight);
    };

    function fitRect(x, y, w, h) {
        
        // Calculate bounds
        var opBoundX = w + x;
        var opBoundY = h + y;
        
        // Determine max width
        var resizeWidth;
        if (opBoundX > width)
            resizeWidth = opBoundX;
        else
            resizeWidth = width;

        // Determine max height
        var resizeHeight;
        if (opBoundY > height)
            resizeHeight = opBoundY;
        else
            resizeHeight = height;

        // Resize if necessary
        if (resizeWidth != width || resizeHeight != height)
            resize(resizeWidth, resizeHeight);

    }

    resize(width, height);

    var readyHandler = null;
    var updates = new Array();
    var autosize = 0;

    function Update(updateHandler) {

        this.setHandler = function(handler) {
            updateHandler = handler;
        };

        this.hasHandler = function() {
            return updateHandler != null;
        };

        this.handle = function() {
            updateHandler();
        }

    }

    display.setAutosize = function(flag) {
        autosize = flag;
    };

    function reserveJob(handler) {
        
        // If no pending updates, just call (if available) and exit
        if (display.isReady() && handler != null) {
            handler();
            return null;
        }

        // If updates are pending/executing, schedule a pending update
        // and return a reference to it.
        var update = new Update(handler);
        updates.push(update);
        return update;
        
    }

    function handlePendingUpdates() {

        // Draw all pending updates.
        var update;
        while ((update = updates[0]).hasHandler()) {
            update.handle();
            updates.shift();
        }

        // If done with updates, call ready handler
        if (display.isReady() && readyHandler != null)
            readyHandler();

    }

    display.isReady = function() {
        return updates.length == 0;
    };

    display.setReadyHandler = function(handler) {
        readyHandler = handler;
    };


    display.drawImage = function(x, y, image) {
        reserveJob(function() {
            if (autosize != 0) fitRect(x, y, image.width, image.height);
            displayContext.drawImage(image, x, y);
        });
    };


    display.draw = function(x, y, url) {
        var update = reserveJob(null);

        var image = new Image();
        image.onload = function() {

            update.setHandler(function() {
                if (autosize != 0) fitRect(x, y, image.width, image.height);
                displayContext.drawImage(image, x, y);
            });

            // As this update originally had no handler and may have blocked
            // other updates, handle any blocked updates.
            handlePendingUpdates();

        };
        image.src = url;

    };

    // Run arbitrary function as soon as currently pending operations complete.
    // Future operations will not block this function from being called (unlike
    // the ready handler, which requires no pending updates)
    display.sync = function(handler) {
        reserveJob(handler);
    }

    display.copyRect = function(srcLayer, srcx, srcy, w, h, x, y) {
  
        function scheduleCopyRect() { 
            reserveJob(function() {
                if (autosize != 0) fitRect(x, y, w, h);
                displayContext.drawImage(srcLayer, srcx, srcy, w, h, x, y, w, h);
            });
        }

        // If we ARE the source layer, no need to sync.
        // Syncing would result in deadlock.
        if (display === srcLayer)
            scheduleCopyRect();

        // Otherwise synchronize copy operation with source layer
        else
            srcLayer.sync(scheduleCopyRect);

    };

    display.clearRect = function(x, y, w, h) {
        reserveJob(function() {
            if (autosize != 0) fitRect(x, y, w, h);
            displayContext.clearRect(x, y, w, h);
        });
    };

    display.filter = function(filter) {
        reserveJob(function() {
            var imageData = displayContext.getImageData(0, 0, width, height);
            filter(imageData.data, width, height);
            displayContext.putImageData(imageData, 0, 0);
        });
    };

    return display;

}

