/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Provides the MenuAction class definition.
 */
angular.module('navigation').factory('MenuAction', [function defineMenuAction() {

    /**
     * Creates a new MenuAction, which pairs an arbitrary callback with
     * an action name. The name of this action will ultimately be presented to
     * the user when the user when this action's associated menu is open.
     *
     * @constructor
     * @param {String} name
     *     The name of this action.
     *
     * @param {Function} callback
     *     The callback to call when the user elects to perform this action.
     * 
     * @param {String} className
     *     The CSS class to associate with this action, if any.
     */
    var MenuAction = function MenuAction(name, callback, className) {

        /**
         * Reference to this MenuAction.
         *
         * @type MenuAction
         */
        var action = this;

        /**
         * The CSS class associated with this action.
         * 
         * @type String
         */
        this.className = className;

        /**
         * The name of this action.
         *
         * @type String
         */
        this.name = name;

        /**
         * The callback to call when this action is performed.
         *
         * @type Function
         */
        this.callback = callback;

    };

    return MenuAction;

}]);
