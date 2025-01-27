# Frontend Extension Example

Adds global css styles, new routes (/guacamole/guacamole-angular-example and /guacamole/guacamole-angular-example/about)
and button below the "Logout" user menu.

[//]: # (TODO: Update for native federation)
## Setup

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) 
version 16.1.1 using the following steps:

```bash
# Create a new minimal Angular project
ng new guacamole-frontend-extension-example \
--minimal \
--prefix guac \
--routing \
--skip-tests \
--skip-install \
--standalone \
--view-encapsulation None

# ... manually set the version of @angular dependencies to "16.1.1" in the package.json

cd guacamole-frontend-extension-example

npm install

# Add support for module federation
ng add @angular-architects/module-federation --project guacamole-frontend-extension-example --type remote --port 4202

# Add the frontend extension library
npm install ..\..\guacamole\src\main\guacamole-frontend\dist\guacamole-frontend-ext-lib

```

Add `"src/app/extension.config.ts"` to `tsconfig.app.json` `files` array.


## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4202/`. The application will automatically reload if you change any of the source files.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.
To package the artifacts for guacamole run `node ./package-extension.js`.
