# Frontend Extension Example

This directory contains an example of a frontend extension for the Angular frontend of Apache Guacamole.
A frontend extension is a Guacamole extension whose user interface is an Angular application.
The frontend loads that application at runtime through Native Federation (`@angular-architects/native-federation`).
Like every Guacamole extension, it is a `.jar` file with a `guac-manifest.json`, installed in `GUACAMOLE_HOME/extensions`.

The example adds the following to Guacamole:

- the page `#/angular-extension` with a field for choosing a planet,
- the page `#/angular-extension/about`, which the frontend loads only when it is opened,
- the form field type `planet-chooser`,
- the link "Goto Extension Page" below the content of every page,
- a CSS rule which colors the application name on the login page.

## How the frontend loads extensions

Guacamole generates two files from the installed extensions:

- `native-federation-manifest.json` maps the namespace of each extension to its `remoteEntry.json`, which Guacamole serves under `app/ext/<namespace>/`.
- `native-federation-config.json` maps the namespace of each extension to its Native Federation configuration (see [Manifest](#manifest)).

On startup, the frontend initializes Native Federation with the manifest.
It then loads the bootstrap function of each extension, calls it and adds the routes it returns below the route path of the extension.
HTML patches of an extension can insert Angular components which the extension exposes.

The frontend and its extensions share Angular, RxJS and `guacamole-frontend-ext-lib` as singletons.
An extension therefore runs in the same Angular application as the frontend, uses the same services, and needs the same versions of these packages (see [Versions](#versions)).

`guacamole-frontend-ext-lib` is the API between the frontend and its extensions.
It exports the type of the bootstrap function (`BootstrapExtensionFunction`), the service for form field types (`FieldTypeService`) and the interfaces `FormField` and `FormFieldComponentData`.
Other services of the frontend, such as its event service, are not part of this API.

## Prerequisites

- The frontend workspace, built as described under [Prerequisites](../../guacamole/src/main/guacamole-frontend/README.md#prerequisites) in the README of the frontend.
  The example installs `guacamole-frontend-ext-lib` from `guacamole/src/main/guacamole-frontend/dist/guacamole-frontend-ext-lib/`.
- Node.js 24 and npm 11, as for the frontend.
- The `zip` command, which `package-extension.js` calls to create the `.jar` file.
- A Guacamole installation whose web application comes from the same sources as the frontend workspace, because the example needs the same versions of the shared packages (see [Versions](#versions)).

## Structure

- `src/app/extension.config.ts`: the bootstrap function `bootstrapExtension`.
- `src/app/app.routes.ts`: the routes of the extension.
- `src/app/components/`: the components of both pages, the link which the HTML patch inserts, and the planet chooser.
- `src/html/button.html`: the HTML patch.
- `src/styles.css`: global CSS.
- `federation.config.mjs`: the Native Federation configuration with the name of the extension, its exposed modules and the shared packages.
- `package-extension.js`: writes the manifest and packages the build as `guacamole-angular-extension.jar`.
- `src/main.ts`, `src/bootstrap.ts`, `src/index.html`, `src/app/app.component.ts` and `src/app/app.config.ts`: start the example as a standalone application in its development server (see [Local development](#local-development)); Guacamole does not use them.

### Bootstrap function

`src/app/extension.config.ts` exports the bootstrap function `bootstrapExtension` of type `BootstrapExtensionFunction`.
The frontend calls it once on startup and passes its injector.
The function registers the field type of the example and returns the routes of the extension; an extension without routes returns an empty array.
To use services of the frontend, inject them within `runInInjectionContext()` with that injector, as the example does with `FieldTypeService`.

`federation.config.mjs` exposes the function under `bootstrapExtension`, the name of its export.
The frontend loads the exposed module of that name and calls its export of the same name, so both names must be equal.
`bootstrapExtension` is the default; another name goes into `bootstrapFunctionName` of the Native Federation configuration.

### Routes

The frontend adds the routes of the extension as children of a route at `routePath` of the Native Federation configuration, `angular-extension` in the example.
`src/app/app.routes.ts` defines the page of the extension at `''`, the page `about`, which `loadComponent()` loads on the first navigation to it, and a redirect from any other path to the page of the extension.
Links within the extension can be relative, such as `../` on the page `about`.
Links from elsewhere must be absolute, such as `/angular-extension` in the link which the HTML patch inserts.

As with the routes of the frontend, the routes of the extension require a valid session.
Without one, the frontend shows the login form and opens the requested route after the login.

While a route of the extension is active, the browser tab shows `pageTitle` of the Native Federation configuration, or the title of Guacamole if it is not set.
A route with its own `title`, such as `about`, overrides it.

As with the routes of the frontend, the router creates the component of a route anew when its path or query parameters change.
Set `data: { reloadOnUrl: false }` on a route to keep its component instead.

### Form field type

The bootstrap function registers `PlanetChooserFieldComponent` under the field type `planet-chooser` with `FieldTypeService.registerFieldType()`.
The forms of Guacamole then render every field of this type with that component.
Such fields come from the REST API, for example as attributes of users or connections which an authentication provider defines.
A field component implements `FormFieldComponentData`; the frontend sets its properties and passes the value of the field as a `FormControl` in `control`.

The example has no Java part which defines a field of this type.
Its page shows the component directly instead.

### HTML patch

`src/html/button.html` is an HTML patch, as known from other Guacamole extensions.
Its element `<meta name="after" content="#other-connections">` inserts the other elements of the patch after the panel of active connections, which follows the content of every page.
The `div` stands for an Angular component: `nativeFederationModule` names the extension by its namespace, and `nativeFederationElement` names the component which the extension exposes.
In place of the `div`, the frontend inserts a new instance of that component.
As with the bootstrap function, the exposed name must equal the name of the export.

The component is standalone and imports every directive it uses, here `RouterLink`.
Each element which the selector matches gets its own instance of the component.
The instance takes part in the change detection of the frontend and is destroyed when its element leaves the DOM.

In the Angular frontend, HTML patches apply to the rendered DOM rather than to templates:

- After each change detection, the frontend applies each patch to the matching elements which have not received it yet, including elements which appear later.
- The frontend does not compile the static HTML of a patch, so Angular and AngularJS syntax in it has no effect.
  Insert a component for dynamic content.
- `before`, `after`, `before-children` and `after-children` suit any element.
  `replace` and `replace-children` apply once per element as well; do not use them on elements which Angular creates anew or whose content Angular renders.

### Styles

The build writes `src/styles.css` into `styles-<hash>.css`, which the manifest lists under `css`.
Guacamole serves the CSS of all extensions as `app.css`, which the frontend loads.
The rule of the example colors the application name on the login page crimson.

## Build and package

Run the following commands in this directory.

Install the npm packages:

```bash
npm ci
```

`npm ci` links `node_modules/guacamole-frontend-ext-lib` to the build output of the library in the frontend workspace, so the example always builds against the latest build of the library.

Build the extension:

```bash
npm run build
```

The build writes `remoteEntry.json`, the exposed modules, the shared packages, the CSS and the HTML patch to `dist/guacamole-frontend-extension-example/`.

Package the build as `guacamole-angular-extension.jar`:

```bash
node package-extension.js
```

`package-extension.js` writes `guac-manifest.json` and `nativeFederationConfiguration.json` into the build output, adds the whole directory to the `.jar` file, replacing one of an earlier build, and deletes both files again.

## Install

Copy `guacamole-angular-extension.jar` to `GUACAMOLE_HOME/extensions`, for example:

```bash
cp guacamole-angular-extension.jar /etc/guacamole/extensions/
```

Restart Tomcat, because Guacamole loads extensions on startup.
The log of Guacamole then contains `Extension "Guacamole Angular Example" (guacamole-angular-example) loaded.`
Log in and follow the link "Goto Extension Page" on the home page.

## Manifest

`package-extension.js` generates `guac-manifest.json` from `CONFIG.manifestTemplate` and the files of the build:

```
{
    "guacamoleVersion": "*",
    "name": "Guacamole Angular Example",
    "namespace": "guacamole-angular-example",
    "css": [
        "styles-<hash>.css"
    ],
    "html": [
        "html/button.html"
    ],
    "resources": {
        "AboutExtensionButtonComponent-<hash>.js": "application/javascript",
        "bootstrapExtension-<hash>.js": "application/javascript",
        "remoteEntry.json": "application/json",
        ...
    },
    "nativeFederationConfiguration": "nativeFederationConfiguration.json"
}
```

- `namespace`: Guacamole serves the resources of the extension under `app/ext/<namespace>/` and lists the extension under its namespace in `native-federation-manifest.json` and `native-federation-config.json`.
  The namespace must equal `name` in `federation.config.mjs`, because Native Federation registers the extension under the name in its `remoteEntry.json`, while the frontend loads its modules by namespace.
- `css` and `html` list the CSS and the HTML patches, as for any Guacamole extension.
- `resources` must contain `remoteEntry.json` and the JavaScript files of the build, which the frontend loads through Native Federation.
  `package-extension.js` lists all `.js` and `.json` files, fonts and images.
- `nativeFederationConfiguration` is the path of the Native Federation configuration within the `.jar` file.
  Without this property, Guacamole lists the extension in neither of the two generated files, and the frontend loads none of its modules.

The other properties of a manifest, such as `translations`, keep their meaning.
Translation strings of an extension can refer to other strings with `@:KEY`, as in the AngularJS frontend, or with `@:KEY:@`, which also works within a longer text.
The Angular frontend does not load the JavaScript which a manifest lists under `js`; the code of a frontend extension comes only through Native Federation.

`package-extension.js` writes the Native Federation configuration `nativeFederationConfiguration.json` from `CONFIG.nativeFederationConfiguration`:

```json
{
    "bootstrapFunctionName": "bootstrapExtension",
    "pageTitle": "Angular Extension",
    "routePath": "angular-extension"
}
```

- `bootstrapFunctionName`: the name under which the extension exposes its bootstrap function; `bootstrapExtension` if not set.
- `pageTitle`: the title of the browser tab on the routes of the extension; optional.
- `routePath`: the path below which the frontend adds the routes of the extension.

If the configuration is missing from the `.jar` file, is no valid JSON or contains unknown properties, Guacamole logs an error and lists the extension in neither of the two generated files.
The rest of the extension still loads.

## Versions

`federation.config.mjs` shares all dependencies with `shareAll({ singleton: true, strictVersion: true, requiredVersion: 'auto' })`, as the frontend does.
Keep the versions of Angular, RxJS, `zone.js`, `tslib`, `es-module-shims`, `@angular-architects/native-federation` and TypeScript in `package.json` and `package-lock.json` equal to those of the frontend in `guacamole/src/main/guacamole-frontend/`, and update them together.

TypeScript resolves the Angular types of `guacamole-frontend-ext-lib` from the npm packages of the frontend workspace, and those of the example from its own.
It merges both only if their versions are equal; otherwise the build fails with errors such as `TS2300: Duplicate identifier`.

The frontend shares only the packages and entry points it imports itself.
If an extension imports others, its build contains its own copy of them.

For `guacamole-frontend-ext-lib`, `requiredVersion: 'auto'` would take the `file:` path from `package.json` as the version range.
`federation.config.mjs` therefore requires the installed version of the library instead.

## Local development

Start the development server of the example:

```bash
npm start
```

It serves the example as a standalone Angular application at `http://localhost:4202/`, with the page of the extension at `/` and the page `about` at `/about`.
On every change, it rebuilds the example and updates the open page.
Without Guacamole, the bootstrap function and the HTML patch do not run, the styles of Guacamole are missing, and the link to `/angular-extension` leads back to `/`.

To try a change within Guacamole, build, package and install the extension again, and restart Tomcat.
The development server of the frontend loads the extensions installed in its backend as well; see [Development server](../../guacamole/src/main/guacamole-frontend/README.md#development-server) in the README of the frontend.
