
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

// Guacamole namespace
var Guacamole = Guacamole || {};

/**
 * Abstract ordered drawing surface. Each Layer contains a canvas element and
 * provides simple drawing instructions for drawing to that canvas element,
 * however unlike the canvas element itself, drawing operations on a Layer are
 * guaranteed to run in order, even if such an operation must wait for an image
 * to load before completing.
 * 
 * @constructor
 * 
 * @param {Number} width The width of the Layer, in pixels. The canvas element
 *                       backing this Layer will be given this width.
 *                       
 * @param {Number} height The height of the Layer, in pixels. The canvas element
 *                        backing this Layer will be given this height.
 */
Guacamole.Layer = function(width, height) {

    // Reference to this Layer
    var layer = this;

    // Off-screen buffer (canvas element) and corresponding
    // context.
    var display = document.createElement("canvas");
    var displayContext = display.getContext("2d");

    /**
     * Returns the canvas element backing this Layer.
     * @returns {Element} The canvas element backing this Layer.
     */
    this.getCanvas = function() {
        return display;
    };

    /**
     * Resizes the canvas element backing this Layer without testing the
     * new size. This function should only be used internally.
     * 
     * @private
     * @param {Number} newWidth The new width to assign to this Layer.
     * @param {Number} newHeight The new height to assign to this Layer.
     */
    function resize(newWidth, newHeight) {
        display.style.position = "absolute";
        display.style.left = "0px";
        display.style.top = "0px";

        display.width = newWidth;
        display.height = newHeight;

        width = newWidth;
        height = newHeight;
    }

    /**
     * Changes the size of this Layer to the given width and height. Resizing
     * is only attempted if the new size provided is actually different from
     * the current size.
     * 
     * @param {Number} newWidth The new width to assign to this Layer.
     * @param {Number} newHeight The new height to assign to this Layer.
     */
    this.resize = function(newWidth, newHeight) {
        if (newWidth != width || newHeight != height)
            resize(newWidth, newHeight);
    };

    /**
     * Given the X and Y coordinates of the upper-left corner of a rectangle
     * and the rectangle's width and height, resize the backing canvas element
     * as necessary to ensure that the rectangle fits within the canvas
     * element's coordinate space. This function will only make the canvas
     * larger. If the rectangle already fits within the canvas element's
     * coordinate space, the canvas is left unchanged.
     * 
     * @private
     * @param {Number} x The X coordinate of the upper-left corner of the
     *                   rectangle to fit.
     * @param {Number} y The Y coordinate of the upper-left corner of the
     *                   rectangle to fit.
     * @param {Number} w The width of the the rectangle to fit.
     * @param {Number} h The height of the the rectangle to fit.
     */
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

    // Initialize canvas dimensions
    resize(width, height);

    /**
     * Set to true if this Layer should resize itself to accomodate the
     * dimensions of any drawing operation, and false (the default) otherwise.
     * 
     * Note that setting this property takes effect immediately, and thus may
     * take effect on operations that were started in the past but have not
     * yet completed. If you wish the setting of this flag to only modify
     * future operations, you will need to make the setting of this flag an
     * operation with sync().
     * 
     * @example
     * // Set autosize to true for all future operations
     * layer.sync(function() {
     *     layer.autosize = true;
     * });
     * 
     * @type Boolean
     * @default false
     */
    this.autosize = false;

    var updates = new Array();

    function Update(updateHandler) {

        this.setHandler = function(handler) {
            updateHandler = handler;
        };

        this.hasHandler = function() {
            return updateHandler != null;
        };

        this.handle = function() {
            updateHandler();
        };

    }

    function reserveJob(handler) {
        
        // If no pending updates, just call (if available) and exit
        if (layer.isReady() && handler != null) {
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
        while ((update = updates[0]) != null && update.hasHandler()) {
            update.handle();
            updates.shift();
        }

    }

    /**
     * Returns whether this Layer is ready. A Layer is ready if it has no
     * pending operations and no operations in-progress.
     * 
     * @returns {Boolean} true if this Layer is ready, false otherwise.
     */
    this.isReady = function() {
        return updates.length == 0;
    };

    /**
     * Draws the specified image at the given coordinates. The image specified
     * must already be loaded.
     * 
     * @param {Number} x The destination X coordinate.
     * @param {Number} y The destination Y coordinate.
     * @param {Image} image The image to draw. Note that this is an Image
     *                      object - not a URL.
     */
    this.drawImage = function(x, y, image) {
        reserveJob(function() {
            if (autosize != 0) fitRect(x, y, image.width, image.height);
            displayContext.drawImage(image, x, y);
        });
    };

    /**
     * Draws the image at the specified URL at the given coordinates. The image
     * will be loaded automatically, and this and any future operations will
     * wait for the image to finish loading.
     * 
     * @param {Number} x The destination X coordinate.
     * @param {Number} y The destination Y coordinate.
     * @param {String} url The URL of the image to draw.
     */
    this.draw = function(x, y, url) {
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

    /**
     * Run an arbitrary function as soon as currently pending operations
     * are complete.
     * 
     * @param {function} handler The function to call once all currently
     *                           pending operations are complete.
     */
    this.sync = function(handler) {
        reserveJob(handler);
    };

    /**
     * Copy a rectangle of image data from one Layer to this Layer. This
     * operation will copy exactly the image data that will be drawn once all
     * operations of the source Layer that were pending at the time this
     * function was called are complete. This operation will not alter the
     * size of the source Layer even if its autosize property is set to true.
     * 
     * @param {Guacamole.Layer} srcLayer The Layer to copy image data from.
     * @param {Number} srcx The X coordinate of the upper-left corner of the
     *                      rectangle within the source Layer's coordinate
     *                      space to copy data from.
     * @param {Number} srcy The Y coordinate of the upper-left corner of the
     *                      rectangle within the source Layer's coordinate
     *                      space to copy data from.
     * @param {Number} srcw The width of the rectangle within the source Layer's
     *                      coordinate space to copy data from.
     * @param {Number} srch The height of the rectangle within the source
     *                      Layer's coordinate space to copy data from.
     * @param {Number} x The destination X coordinate.
     * @param {Number} y The destination Y coordinate.
     */
    this.copyRect = function(srcLayer, srcx, srcy, srcw, srch, x, y) {

        function doCopyRect() {
            if (autosize != 0) fitRect(x, y, srcw, srch);
            displayContext.drawImage(srcLayer, srcx, srcy, srcw, srch, x, y, srcw, srch);
        }

        // If we ARE the source layer, no need to sync.
        // Syncing would result in deadlock.
        if (layer === srcLayer)
            reserveJob(doCopyRect);

        // Otherwise synchronize copy operation with source layer
        else {
            var update = reserveJob(null);
            srcLayer.sync(function() {
                
                update.setHandler(doCopyRect);

                // As this update originally had no handler and may have blocked
                // other updates, handle any blocked updates.
                handlePendingUpdates();

            });
        }

    };

    this.clearRect = function(x, y, w, h) {
        reserveJob(function() {
            if (autosize != 0) fitRect(x, y, w, h);
            displayContext.clearRect(x, y, w, h);
        });
    };

    this.filter = function(filter) {
        reserveJob(function() {
            var imageData = displayContext.getImageData(0, 0, width, height);
            filter(imageData.data, width, height);
            displayContext.putImageData(imageData, 0, 0);
        });
    };

    var compositeOperation = {
     /* 0x0 NOT IMPLEMENTED */
        0x1: "destination-in",
        0x2: "destination-out",
     /* 0x3 NOT IMPLEMENTED */
        0x4: "source-in",
     /* 0x5 NOT IMPLEMENTED */
        0x6: "source-atop",
     /* 0x7 NOT IMPLEMENTED */
        0x8: "source-out",
        0x9: "destination-atop",
        0xA: "xor",
        0xB: "destination-over",
        0xC: "copy",
     /* 0xD NOT IMPLEMENTED */
        0xE: "source-over",
        0xF: "lighter"
    };

    this.setChannelMask = function(mask) {
        reserveJob(function() {
            displayContext.globalCompositeOperation = compositeOperation[mask];
        });
    };

}

