const { withNativeFederation, shareAll } = require('@angular-architects/native-federation/config');

module.exports = withNativeFederation({

  name: 'guacamole-frontend-extension-example',

  exposes: {
    'bootsrapExtension': './src/app/extension.config.ts',
    'AboutExtensionButtonComponent': './src/app/components/about-extension-button.component.ts'
  },

  shared: {
    ...shareAll({ singleton: true, strictVersion: true, requiredVersion: 'auto' }),
  },

  // Packages that should not be shared or are not needed at runtime
  skip: [
    'rxjs/ajax',
    'rxjs/fetch',
    'rxjs/testing',
    'rxjs/webSocket'
  ]

  // Please read our FAQ about sharing libs:
  // https://shorturl.at/jmzH0
  
});
