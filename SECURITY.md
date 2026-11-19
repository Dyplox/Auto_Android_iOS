# Security Policy

## Supported versions

Security fixes are made on `main` and shipped in the next release. Only the latest release is supported.

| Version                                                                  | Supported |
|--------------------------------------------------------------------------|-----------|
| Latest [release](https://github.com/Dyplox/Auto_Android_iOS/releases) | ✅        |
| Older releases                                                           | ❌        |

## Reporting a vulnerability

**Do not open a public issue.** Report it privately through GitHub:
[*Security › Report a vulnerability*](https://github.com/Dyplox/Auto_Android_iOS/security/advisories/new).

Please include what is affected (file, workflow or dependency), how to reproduce it and the impact you expect.

What to expect:

- An acknowledgement within **5 business days**.
- An assessment (accepted or declined, with the reason) within **15 business days**.
- If it is accepted, a fix on `main` and a new release. You will be credited in the advisory unless you prefer not
  to be.

## Scope

This repository is a test automation framework: it ships no application and stores no user data. Relevant reports
include, for example:

- Leaks of secrets (`SONAR_TOKEN`, BrowserStack or Sauce Labs credentials) through code, logs, reports or CI
  artifacts.
- Weaknesses in the GitHub Actions workflows (excessive token permissions, script injection, unpinned actions).
- Supply-chain issues: dependency verification bypasses or compromised dependencies (Gradle, npm, Actions).
- Unsafe handling of downloaded files in the `scripts/` folder.

Out of scope: vulnerabilities in the app under test ([WebdriverIO Native Demo App](https://github.com/webdriverio/native-demo-app))
or in third-party services; report those to their maintainers.

## How the project protects itself

- GitHub Actions pinned to commit SHAs and a read-only `GITHUB_TOKEN` by default.
- Gradle dependencies locked (`*.lockfile`) and verified against SHA-256 checksums (`gradle/verification-metadata.xml`);
  `npm ci --ignore-scripts` with `package-lock.json`.
- Credentials only from environment variables, `.env` (ignored by Git) or repository secrets.
- SonarQube Cloud analysis with a Quality Gate on every pull request, and weekly Dependabot updates.
