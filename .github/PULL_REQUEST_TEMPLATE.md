<!-- One concern per PR. See CONTRIBUTING.md. -->

## What & why

Describe the change and the motivation. Link the issue (`Closes #…`).

## How verified

- [ ] Unit tests passed (`:mobile:testDebugUnitTest`, `:shared:testDebugUnitTest`, and affected modules)
- [ ] APK build passed (`:assembly:assembleCompleteNormalDebug`; include `:probe:assembleDebug` if PrismProbe changed)
- [ ] On-device checked (required for UI/behaviour changes) — describe what
- [ ] No success/normal UI state shown without the real backend result

## Scope / safety

- [ ] Diff is minimal; no unrelated reformatting
- [ ] Protected boundaries unchanged (`shared/`, `engine/`, `installer/`,
      public API, profile provisioning, clone/install kernel) — or a spec +
      reviewer sign-off is attached
- [ ] Licensing/attribution preserved
- [ ] User-facing behavior is documented or explained in the PR when relevant

## Notes for reviewers
