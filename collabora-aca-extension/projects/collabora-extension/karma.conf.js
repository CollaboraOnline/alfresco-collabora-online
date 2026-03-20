// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html
const { join } = require('path');
const getBaseKarmaConfig = require('../../karma.conf');

module.exports = function (config) {
  const baseConfig = getBaseKarmaConfig();
  config.set({
    ...baseConfig,
    coverageReporter: {
      ...baseConfig.coverageReporter,
      dir: join(__dirname, '../../coverage/collabora-extension'),
      reporters: [{ type: 'html' }, { type: 'text-summary' }],
      thresholds: {
        global: {
          statements: 20,
          branches: 0,
          functions: 20,
          lines: 20
        }
      }
    },
    files: [
      '../../node_modules/katex/dist/katex.min.js',
      '../../node_modules/katex/dist/contrib/auto-render.min.js',
      '../../node_modules/katex/dist/katex.min.css',
      '../../node_modules/mermaid/dist/mermaid.min.js'
    ]
  });
};
