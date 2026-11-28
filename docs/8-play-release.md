# 8. Play release pipeline

> **Status:** Implemented on `feature/play-release-pipeline` (2026-11-28), checked locally ([§6](#6-implementation-and-checks)); the first `-alpha` and `-production` tags are the end-to-end checks · **Same pipeline as:** MyVNDB's `docs/plans/18-play-release-pipeline.md`, where it was designed and first released

Pushing a tag `v<versionName>-<versionCode>-<track>` publishes BakaMitai (`com.zc.bakamitai`) to that Google Play track:

- **`-internal`, `-alpha`, `-beta`:** `.github/workflows/play-release.yml` builds `productionRelease` as an App Bundle with that version, signs it with the upload key and uploads it, with the tag's message as the release notes.
- **`-production`:** `.github/workflows/play-promote.yml` promotes the build Alpha has with that version code. Nothing is built; it checks out only `Gemfile` and `Gemfile.lock`.

Starting point: the app is live on production. Closed testing – Alpha exists with no releases yet, so the first `-alpha` tag creates its first release.

---

## 1. Tags

| Tag | Track | Workflow |
|---|---|---|
| `v2.0.1-6-internal` | Internal testing | Play release |
| `v2.0.1-6-alpha` | Closed testing – Alpha | Play release |
| `v2.0.1-6-beta` | Open testing | Play release |
| `v2.0.1-6-production` | Production | Play promote |

- Format: `^v([0-9]+\.[0-9]+\.[0-9]+)-([1-9][0-9]{0,9})-(internal|alpha|beta|production)$`. Play release starts on every `v<x.y.z>-<code>-*` tag except `-production`, so a mistyped track fails with a clear error.
- Play takes each version code once, across all tracks: every build needs a higher code than any uploaded so far.
- Release notes: an annotated tag's message, en-US, at most 500 characters, line breaks included (`git tag -a <tag> -F - <<'EOF' … EOF`). A lightweight tag gets "Bug fixes and improvements." A production release keeps its Alpha notes.
- A production tag promotes only the build Alpha currently has with that code. If Alpha doesn't have it, the run fails and nothing changes on Play. Fixes reach production through Alpha.

## 2. Gradle

- `app/build.gradle.kts` reads `versionCode` / `versionName` from Gradle properties (`-PversionCode=6 -PversionName=2.0.1`), defaulting to 5 / `2.0.0`.
- `productionRelease` has no signing config. The workflow signs it through AGP's own `android.injected.signing.store.file`, `…store.password`, `…key.alias` and `…key.password` (the properties Android Studio's Generate Signed App Bundle uses; present in AGP 9.5.1). Never put them in `~/.gradle/gradle.properties`: every project and build type on the machine would be signed with the upload key.
- `developmentRelease` and `build-release-apk.yml` are unchanged.

## 3. Workflows

Everything app-specific sits in each file's top `env:` block: `APP_ID`, `MODULE`, `VARIANT`, `TEST_TASK` (Play release) and `APP_ID`, `PROMOTE_FROM` (Play promote). Both share the `google-play` concurrency group, so they never edit Play at the same time.

**Play release** (`parse` job, then `publish` in the `google-play` environment): checkout → release notes from the tag into `$RUNNER_TEMP/metadata/en-US/changelogs/<versionCode>.txt` → JDK 25, Gradle → decode the upload keystore → `testDevelopmentDebugUnitTest` and `:app:bundleProductionRelease` with the version and injected signing → fail if the bundle is unsigned, keep the bundle and mapping as a run artifact → Ruby 3.4 and fastlane → `fastlane supply --track <track> --aab … --mapping … --release_status completed --metadata_path …`, skipping the listing text, images and screenshots.

**Play promote** (one `promote` job in the `google-play` environment): read the tag → sparse checkout of `/Gemfile` and `/Gemfile.lock` → Ruby 3.4 and fastlane → `fastlane supply --track alpha --track_promote_to production --version_code <code>`, skipping every metadata upload. It never gets the upload keystore secrets.

fastlane (2.242.0, `Gemfile.lock` for `arm64-darwin`, `ruby` and `x86_64-linux`) is CI tooling, not an app dependency, so it isn't in the version catalog or `tech-stack`.

## 4. Setup (once)

### A. Play Console

1. **Highest version code so far:** **App bundle explorer** lists every bundle on every track. `app/build.gradle.kts` defaults to 5, so the first tag probably needs 6 or more.
2. **Alpha testers:** Alpha has no releases yet. Add an email list or Google Group under **Closed testing – Alpha → Testers** so someone receives the first build. Uploading works without them.
3. **Listing language:** the release notes go up as `en-US`. If BakaMitai's default listing language is different, change `en-US` in the "Release notes from the tag" step of `play-release.yml`.

### B. The upload key

Use the keystore that signed BakaMitai's production uploads; Play rejects bundles signed with any other key. Check it:

```bash
keytool -list -v -keystore <path-to-keystore> -alias <alias>
```

Its SHA-256 must match Play Console → **App integrity → App signing → Upload key certificate**. If it's lost, request an upload key reset there.

### C. Service account

Reuse MyVNDB's: `github-play-publisher@avid-renderer-354814.iam.gserviceaccount.com`, with the same JSON key. In Play Console → **Users and permissions** → that account → **App permissions → Add app → BakaMitai**, tick:

- View app information and download bulk reports (read-only)
- Release apps to testing tracks
- Release to production, exclude devices, and use Play App Signing

Permissions can take a few hours to apply; until then uploads fail with "The caller does not have permission".

### D. GitHub (`zahichemaly/bakamitai-app`)

1. **Settings → Environments → New environment** `google-play` → **Deployment branches and tags → Selected branches and tags → Add rule → Tag** `v*.*.*-*-*`.
2. Secrets **on the environment** (not the repository), from the files themselves:
   ```bash
   gh secret set PLAY_SERVICE_ACCOUNT_JSON --env google-play -R zahichemaly/bakamitai-app < <path-to-service-account-key>.json
   ```
   ```bash
   base64 -i <path-to-keystore> | gh secret set UPLOAD_KEYSTORE_BASE64 --env google-play -R zahichemaly/bakamitai-app
   ```
   ```bash
   gh secret set UPLOAD_KEYSTORE_PASSWORD --env google-play -R zahichemaly/bakamitai-app
   ```
   ```bash
   gh secret set UPLOAD_KEY_ALIAS --env google-play -R zahichemaly/bakamitai-app
   ```
   ```bash
   gh secret set UPLOAD_KEY_PASSWORD --env google-play -R zahichemaly/bakamitai-app
   ```
3. Optional: a tag ruleset on `v*` (only you create, move or delete release tags), and **Required reviewers** on the environment.

### E. First release

Tag a commit that has the workflows (this branch, or `develop` once merged), with a code above A.1:

```bash
git tag -a v2.0.1-6-alpha -F - <<'EOF'
What's new in this release.
EOF
```

```bash
git push origin v2.0.1-6-alpha
```

Then, once tested on Alpha:

```bash
git tag v2.0.1-6-production
```

```bash
git push origin v2.0.1-6-production
```

## 5. When it fails

| Error | Cause | Fix |
|---|---|---|
| `… doesn't seem to be a JSON file` | `PLAY_SERVICE_ACCOUNT_JSON` isn't the key file's exact content | Set it again from the file with `gh secret set … < key.json` |
| The caller does not have permission | BakaMitai not added to the service account's app permissions, or still propagating | §4.C, wait a few hours |
| Version code N has already been used | That code is already on Play | New tag with a higher code |
| Track 'alpha' doesn't have any releases (production tag) | Alpha doesn't have that version code | Check Alpha → Releases; tag the code it has |
| The Android App Bundle was signed with the wrong key | `UPLOAD_KEYSTORE_BASE64` isn't the keystore that signed earlier uploads | §4.B |
| Keystore was tampered with, or password was incorrect | Wrong `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS` or `UPLOAD_KEY_PASSWORD` | Fix the secret, re-run the failed job |
| `'…' is not v<x.y.z>-<versionCode>-<internal\|alpha\|beta>` | Mistyped track, or a zero-padded code | Delete the tag, push the right one |
| Fails before the upload or promotion | Tests, build or a missing secret | Fix, then **Re-run failed jobs** (secrets are read at run time) or delete and re-push the tag |

## 6. Implementation and checks

Changed on `feature/play-release-pipeline` (from `develop`):
- `app/build.gradle.kts`: the version properties and the `productionRelease` signing comment.
- `.github/workflows/play-release.yml`, `.github/workflows/play-promote.yml`, `Gemfile` and `Gemfile.lock`: copied from MyVNDB, with only the `env:` values changed (`APP_ID: com.zc.bakamitai`) and the doc link.
- `.gitignore`: Bundler folders.
- `README.md`: a "Releasing" section.
- `CLAUDE.md`: signing, version and release lines.

Checked locally (2026-11-28):
- **Build:** the workflow's own build step (taken from the YAML with `yq`) ran with a throwaway JKS key whose key password differs from the store password. `testDevelopmentDebugUnitTest` passed, and `bundleProductionRelease` was signed by that key, with `versionCode` 12345 and `versionName` 9.8.7 from the properties. The find-and-check step found the bundle (`app-production-release.aab`) and the mapping.
- **Defaults:** without properties, the production bundle is unsigned, with `versionCode` 5 and `2.0.0`. `assembleDevelopmentRelease` still gives 5 / `2.0.0-dev`, signed with `debug.keystore`.
- **Workflows:** both files parse. Apart from `env:`, they are identical to MyVNDB's, whose tag routing, notes, key and sparse-checkout steps were checked there and released to Alpha. `Gemfile.lock` is identical too.
- **fastlane:** installed from the lock, and the upload and promote commands from these files both parse with `com.zc.bakamitai`, stopping at the missing key file before any network call.

Not checkable here: talking to Google Play needs §4 done. The first `-alpha` tag checks the upload, and the first `-production` tag checks promotion.
