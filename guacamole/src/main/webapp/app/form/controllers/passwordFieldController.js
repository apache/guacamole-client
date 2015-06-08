/*
 * Copyright (C) 2015 Glyptodon LLC
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
 * Controller for password fields.
 */
angular.module('form').controller('passwordFieldController', ['$scope',
    function passwordFieldController($scope) {

    /**
     * The type to use for the input field. By default, the input field will
     * have the type 'password', and thus will be masked.
     *
     * @type String
     * @default 'password'
     */
    $scope.passwordInputType = 'password';

    /**
     * Returns a string which describes the action the next call to
     * togglePassword() will have.
     *
     * @return {String}
     *     A string which describes the action the next call to
     *     togglePassword() will have.
     */
    $scope.getTogglePasswordHelpText = function getTogglePasswordHelpText() {

        // If password is hidden, togglePassword() will show the password
        if ($scope.passwordInputType === 'password')
            return 'FORM.HELP_SHOW_PASSWORD';

        // If password is shown, togglePassword() will hide the password
        return 'FORM.HELP_HIDE_PASSWORD';

    };

    /**
     * Toggles visibility of the field contents, if this field is a
     * password field. Initially, password contents are masked
     * (invisible).
     */
    $scope.togglePassword = function togglePassword() {

        // If password is hidden, show the password
        if ($scope.passwordInputType === 'password')
            $scope.passwordInputType = 'text';

        // If password is shown, hide the password
        else
            $scope.passwordInputType = 'password';

    };

}]);
