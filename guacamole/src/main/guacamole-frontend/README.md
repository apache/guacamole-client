# Guacamole Angular Frontend

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you
change any of the source files.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a
package that implements end-to-end testing capabilities.

## Development notes

- AngularJS services that mostly serve as data containers are replaced by simple classes with instance or static
  methods. Some of the more complex services are replaced by classes and Angular services.
- Named functions are mostly replaced by arrow functions.
- Functions that return a promise with data from REST API are mostly replaced by functions that return an observable.
- AngularJS directives are replaced by Angular...
    - components if they are configured with `restrict: 'E'`,
    - directives if they are configured with `restrict: 'A'`.
- The authentication service method to perform HTTP request with the current token (`AuthenticationService.request()`)
  is replaced by a HTTP interceptor
  (`auth/interceptor/authentication.interceptor.ts`).
- The error handling of the HTTP requests (`rest/services/requestService.js`) is replaced by a HTTP interceptor
  (`rest/interceptor/error-handling.interceptor.ts`).
- The Configuration of the $http service (`httpDefaults.js`) is replaced by a HTTP
  interceptor (`index/config/DefaultHeadersInterceptor.ts`).
- To disable certain interceptors for specific requests, the HttpContextTokens in the `InterceptorService` can be used.
- The angular-translate library is replaced by transloco (https://ngneat.github.io/transloco/).
    - A noticeable difference is that transloco uses double curly braces by default to link to other translations keys
      instead of a `@:` prefix.

- The 'import' module is not included in this first version.
- Additionally, the following files are also not migrated yet:
    - index/services/iconService.js
- If the user navigates to a route without being authenticated, the user is redirected to the home page after
  authentication.
- I'm not sure how to handle the automatic generation of the LICENSE and NOTICE files in the maven build.
  The angular build creates a `3rdpartylicenses.txt` file, but I don't know if this is enough and where to put it.
- Type declarations for guacamole-common-js (`guacamole-frontend-lib/src/lib/types/Guacamole.ts`) will be replaced by the package @types/guacamole-common-js
  once it is updated.
- I included some Cypress E2E Tests in the `cypress` folder. They are not integrated in the build process and I did not
  include a proper configuration file because of the various possible testing setups.
- I refactored the GuacFileBrowser to simply use an *ngFor loop. For my tests I didn't notice any performance
  difference issues.
## Possible discussion points regarding the implementation

- I removed workarounds for IE since Angular itself does no longer support IE. Should these IE workarounds still be
  kept?
    - `TunnelService~uploadToStream`
    - `TunnelService~downloadStream`
    - `UserCredentials.getLink`
    - `UserCredentialService~getLink`

- To replace the `$parse` function of AngularJS I used the npm package angular-expressions.
  It is basically a copy of the AngularJS code as standalone module.
  If there is any problem with this package as a dependency, we have to invest more time to implement the parsing of the
  expressions ourselves or refactor the code to not use expressions at all.
