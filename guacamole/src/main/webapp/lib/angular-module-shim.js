
/**
 * Workaround to make defining and retrieving angular modules easier and more intuitive.
 * https://gist.github.com/hiddentao/7300694
 */
  
(function(angular) {
  var origMethod = angular.module;
  
  var alreadyRegistered = {};
  
  /**
   * Register/fetch a module.
   *
   * @param name {string} module name.
   * @param reqs {array} list of modules this module depends upon.
   * @param configFn {function} config function to run when module loads (only applied for the first call to create this module).
   * @returns {*} the created/existing module.
   */
  angular.module = function(name, reqs, configFn) {
   reqs = reqs || [];
   var module = null;
  
    if (alreadyRegistered[name]) {
      module = origMethod(name);
      module.requires.push.apply(module.requires, reqs);
    } else {
      module = origMethod(name, reqs, configFn);
      alreadyRegistered[name] = module;
    }
  
    return module;
  };
  
})(angular);
