# Guacamole Angular Frontend

This Angular workspace contains the web interface of Apache Guacamole.
It has three projects:

- `guacamole-frontend`: the application, which Maven packages into the Guacamole web application (WAR).
- `guacamole-frontend-lib`: components, directives and services of the Guacamole client, together with guacamole-common-js and the keyboard layouts.
- `guacamole-frontend-ext-lib`: the API for frontend extensions.

Run the Maven commands below in the root of the repository and all other commands in this directory (`guacamole/src/main/guacamole-frontend`).

## Prerequisites

Build the web application once with Maven:

```bash
mvn -pl guacamole -am package
```

The build compiles guacamole-common-js and copies its `all.min.js` into `projects/guacamole-frontend-lib/src/assets/`.
It then installs the npm packages, builds both libraries into `dist/`, builds the application, runs the unit tests and packages everything as `guacamole/target/guacamole-<version>.war`.
`mvn package` builds the same and all extensions in addition.
Running Maven in `guacamole/` alone works only if guacamole-common-js and guacamole-ext of the same version are already in your local Maven repository.

The npm commands need Node.js 24 and npm 11, the versions the Maven build uses.
The Maven build installs both into `guacamole/target/node/`; add that directory to your `PATH` if you have no matching installation.

## Build

To build the frontend without Maven, build the libraries first, because the application imports `guacamole-frontend-lib` from `dist/`:

```bash
npx ng build guacamole-frontend-lib
npx ng build guacamole-frontend-ext-lib
npm run build:app
```

The application is written to `dist/guacamole-frontend/`.
Extensions build against `dist/guacamole-frontend-ext-lib/`, while the application compiles that library from its sources.

## Development server

The development server serves the application at `http://localhost:4200/` and forwards backend requests to a Guacamole web application at `http://localhost:8080/guacamole/`.

1. Deploy `guacamole/target/guacamole-<version>.war` as `guacamole.war` to a Tomcat on port 8080, together with guacd and an authentication configuration.
   The manual describes these steps under [Installing Guacamole natively](https://guacamole.apache.org/doc/gug/guacamole-native.html) and [Configuring Guacamole](https://guacamole.apache.org/doc/gug/configuring-guacamole.html).
2. Start the development server:

   ```bash
   npm start
   ```

3. Open `http://localhost:4200/` and log in with an account of the backend.

The proxy configuration `projects/guacamole-frontend/src/proxy.conf.json` forwards `api`, `tunnel`, `websocket-tunnel`, `native-federation-manifest.json`, `native-federation-config.json`, `app.css`, `app/ext` and `translations` to the backend.
Everything else comes from the development server.
For a backend at a different address, change `target` and `pathRewrite` in that file.

Translations come from the backend, merged with those of the installed extensions, as in a deployed Guacamole.
The build configuration `serve` in `angular.json` leaves them out of the assets of the development server, which would otherwise answer these requests itself.
Changes to `projects/guacamole-frontend/src/translations/` therefore appear only after you rebuild the WAR and deploy it again.

The development server rebuilds the application and `guacamole-frontend-ext-lib` on every change.
After changing `guacamole-frontend-lib`, stop the development server, rebuild the library with `npx ng build guacamole-frontend-lib` and start the server again.

Extensions installed in the backend load in the development server as well.
To build and install one, follow the [README of the example extension](../../../../doc/guacamole-frontend-extension-example/README.md).

## Unit tests

The unit tests run with [Vitest](https://vitest.dev) and jsdom and need no browser.
Run all tests of both projects once:

```bash
npm test
```

Run the tests of one project in watch mode, which repeats them on every change:

```bash
npx ng test guacamole-frontend
npx ng test guacamole-frontend-lib
```

Run a single spec file with `--include`, which also takes directories and globs, or select tests by name with a regular expression in `--filter`:

```bash
npx ng test guacamole-frontend --watch=false --include projects/guacamole-frontend/src/app/util/order-by.pipe.spec.ts
npx ng test guacamole-frontend --watch=false --filter 'OrderByPipe'
```

The application tests import `guacamole-frontend-lib` from `dist/`; rebuild the library after changing it.
Maven runs the tests of both projects in its `test` phase; `-DskipTests` skips them.

## Lint

Check all three projects with ESLint:

```bash
npm run lint
```
