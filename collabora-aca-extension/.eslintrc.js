const path = require('path');
module.exports = {
  root: true,
  ignorePatterns: ['dist/**', 'coverage/**', 'out-tsc/**', 'node_modules/**'],
  overrides: [
    {
      files: ['*.ts'],
      parserOptions: {
        project: [path.join(__dirname, 'tsconfig.lib.json'), path.join(__dirname, 'tsconfig.spec.json')],
        createDefaultProgram: true
      },
      extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:@angular-eslint/recommended'
      ],
      rules: {}
    },
    {
      files: ['*.html'],
      extends: ['plugin:@angular-eslint/template/recommended'],
      rules: {}
    }
  ]
};
