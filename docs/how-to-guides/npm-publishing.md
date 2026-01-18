# How to Publish Vibium to npm

## Prerequisites

- npm account with access to `vibium` package
- Member of `@vibium` org on npm
- Logged in (`npm login`)

## Build, Test & Package

```bash
make build
make test
make package
```

## Local Testing (Before Publishing)

Always test locally before publishing:

```bash
# Pack the main package
cd packages/vibium && npm pack

# Test in a fresh directory
mkdir /tmp/vibium-test && cd /tmp/vibium-test
npm init -y
npm install /path/to/vibium/packages/vibium/vibium-*.tgz

# Verify it works
node -e "const { browser } = require('vibium'); console.log('OK')"
npx vibium  # Should start MCP server
```

## Publishing

**Important:** Publish platform packages first, then main package.

```bash
# Platform packages (all must succeed before publishing main)
(cd packages/linux-x64 && npm publish --access public)
(cd packages/linux-arm64 && npm publish --access public)
(cd packages/darwin-x64 && npm publish --access public)
(cd packages/darwin-arm64 && npm publish --access public)
(cd packages/win32-x64 && npm publish --access public)

# Main package (after all platform packages are live)
(cd packages/vibium && npm publish)
```

## Version Bumping

All packages must have matching versions. Update all package.json files together:

```bash
# Files to update:
# - clients/javascript/package.json
# - packages/vibium/package.json (version + optionalDependencies versions)
# - packages/linux-x64/package.json
# - packages/linux-arm64/package.json
# - packages/darwin-x64/package.json
# - packages/darwin-arm64/package.json
# - packages/win32-x64/package.json
# - package-lock.json (run `npm install` to regenerate)
```

## Troubleshooting

### "You must be logged in to publish"
```bash
npm login
npm whoami  # Verify you're logged in
```

### "Package name too similar to existing package"
The `@vibium` scope prevents this. Ensure you're using scoped names.

### "Cannot publish over previously published version"
Bump the version number. npm doesn't allow republishing the same version.
