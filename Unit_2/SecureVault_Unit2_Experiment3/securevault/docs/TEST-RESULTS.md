# Verification results

Verified on 13 October 2026, using Java 17 on Linux.

## Automated security integration suite

**15 tests passed; zero failures, zero errors, zero skipped.**

Command: `mvn test` (also run as part of `mvn package`).

| Test | Result |
|---|---|
| rolesAreEnforcedByBackend | PASS |
| staticDashboardIsPublicAndHasSecurityHeaders | PASS |
| aesUsesRandomNoncesAndRejectsTamperingOrWrongContext | PASS |
| rotationRejectsReplayAndRevokesReplacement | PASS |
| csrfIsRequiredIncludingLoginAndRefresh | PASS |
| revokeAllStopsEveryRefreshSession | PASS |
| expiredRefreshTokensAreRejected | PASS |
| bearerRequestsDoNotClearBrowserCsrfCookie | PASS |
| logoutRevokesRefreshButJwtRemainsValidUntilExpiry | PASS |
| unauthenticatedAndTamperedRequestsFail | PASS |
| onlyAdminCanCreateAccountsAndPasswordsAreHashed | PASS |
| expiredJwtAndWrongAudienceAreRejected | PASS |
| invalidVaultInputAndIncorrectPasswordAreRejected | PASS |
| vaultEncryptsAtRestAndIsolatesOwners | PASS |
| browserCsrfCookieFlowWorks | PASS |

These tests use the real Spring application context, security filter chain, controller methods, CSRF cookie/header mechanism, encryption service, and an isolated H2 database. They do not mock authentication decisions. The Mockito subclass configuration avoids requiring JVM self-attachment in restricted test environments.

## Packaged application: real HTTP checks

- PASS — Packaged JAR starts; real HTTP login and CSRF cookie flow succeed
- PASS — Dashboard HTML, JavaScript, CSS and SVG served successfully
- PASS — Real HTTP save, encrypted-value inspection and owner decryption succeed
- PASS — Real HTTP refresh cookie rotation issues a usable access JWT
- PASS — File-backed database and encryption keys survive application restart
- PASS — Real HTTP deletion and refresh rejection after logout succeed

The packaged-JAR checks launched the actual embedded Tomcat server twice, using a temporary working directory. Test database files and encryption keys are not included in the deliverable.

## Additional checks

- JavaScript passes `node --check`.
- Postman collection parses as valid JSON and contains 14 ordered requests.
- The ZIP contains source, documentation, tests, launch scripts and a prebuilt executable JAR.

## Verification limits

- Browser automation and visual rendering were unavailable; responsive layout and browser interactions have not been tested in an actual browser here.
- The Postman collection was prepared and structurally checked; Postman itself was not run. Equivalent backend flows were tested using MockMvc and real HTTP.
- Windows start.bat is included for convenience but was not executed on Windows. The same packaged JAR was started and checked on Linux.
- This is a classroom application. No production deployment, penetration test, external OAuth-provider connection or load test was performed.
