# Contributing to PrismSpace

Thanks for your interest. Please keep changes focused, reviewed, and easy to
verify.

## Ground rules

- **License**: contributions are accepted under the project license,
  **GPL-3.0** (see `LICENSE`).
- **Scope of use**: PrismSpace is for app dual-open on devices you own or are
  authorized to manage.
- **Protected boundaries (keep focused unless explicitly agreed):** `shared/`,
  `engine/`, `installer/`, public API surfaces, profile provisioning, and the
  clone/install kernel. Changes here need a spec and explicit review.

## Workflow

1. Open an issue describing the change first for anything non-trivial.
2. Branch from the working branch; keep one concern per PR.
3. Follow the existing architecture: UI (Compose, pure render) → ViewModel
   (StateFlow, no blocking IPC) → Repository/UseCase → controller/engine.
   Presentation logic should be pure and unit-tested.
4. Run the local gate before pushing:
   ```bash
   ./gradlew :mobile:testDebugUnitTest :shared:testDebugUnitTest :probe:testDebugUnitTest
   ./gradlew :assembly:assembleCompleteNormalDebug :probe:assembleDebug
   ```
   Add focused tests for behavior changes. Use a real device for UI, profile,
   install, or cross-profile behavior changes.
5. Commit messages: `type(scope): summary` (e.g. `fix(prismspace): …`),
   imperative mood, explain *why* in the body for non-trivial changes.

## Pull requests

- Describe what changed, why, and how you verified it (tests + on-device where
  UI/behaviour changed).
- Keep diffs minimal; do not reformat unrelated code.
- UI/behaviour changes should include the verification you ran; honesty of
  user-facing state is a hard requirement (no success message without backend
  success, no vague copy hiding platform limits).
- CI / reviewers may request an independent second review for risky changes.

## Reporting bugs / requesting features

Use the issue templates and include the PrismSpace version, device model,
Android version, run mode, screenshots or diagnostic logs when relevant.
