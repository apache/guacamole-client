# guacamole-frontend-ext-lib

This library is the API between the Guacamole frontend and its extensions: the type of the bootstrap function through which the frontend loads an extension, and the form field types that extensions register with `FieldTypeService`.
The application shares the library with extensions through Native Federation, so that both use the same instance of its services.
Extensions build against `dist/guacamole-frontend-ext-lib/`; the example extension is in `doc/guacamole-frontend-extension-example/`.

The build is described in the [README of the frontend](../../README.md).
