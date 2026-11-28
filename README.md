# BakaMitai

View the latest daily and weekly anime releases from [SubsPlease](https://subsplease.org/). Bookmark a show to be notified of upcoming releases via push notifications.

> **Status:** the app is being rewritten from scratch. `develop` holds a fresh Jetpack Compose scaffold; the previous version is on `master` and in the git history.

## Tech stack

- Kotlin, Jetpack Compose and Material 3 (with Material 3 Adaptive for larger screens)
- Clean Architecture with MVI, single `:app` module organized by feature
- Planned: Navigation 3, Koin, Ktor Client, kotlinx.serialization, DataStore, Coil 3, kotlinx-datetime
- Quality: Detekt with Compose rules, JUnit 4, kotlinx-coroutines-test, Turbine

The full list of approved libraries, and which layer may use each, is in [`.claude/skills/tech-stack`](.claude/skills/tech-stack/SKILL.md).

## Requirements

- JDK 17 or newer
- Android SDK with the API 37 platform
- A device or emulator running Android 8.1 (API 27) or newer

## Getting started

```bash
git clone https://github.com/zahichemaly/BakaMitai.git
```

```bash
cd BakaMitai
```

```bash
./gradlew installDevelopmentDebug
```

Or open the project in Android Studio and run the `app` configuration.

## Tests

```bash
./gradlew testDevelopmentDebugUnitTest
```

```bash
./gradlew connectedDevelopmentDebugAndroidTest
```

Instrumented tests need a running emulator or connected device.

## Releasing

Pushing a tag `v<versionName>-<versionCode>-<track>` publishes to that Google Play track, through
[fastlane](https://docs.fastlane.tools/actions/supply/) pinned in `Gemfile.lock`.

| Track        | Google Play            | Workflow                          | What the tag does                                                                                                                |
|--------------|------------------------|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `internal`   | Internal testing       | Play release (`play-release.yml`) | Builds the `productionRelease` bundle with that version, signs it with the Play upload key and uploads it                        |
| `alpha`      | Closed testing – Alpha | Play release                      | Same                                                                                                                             |
| `beta`       | Open testing           | Play release                      | Same                                                                                                                             |
| `production` | Production             | Play promote (`play-promote.yml`) | Promotes the build Alpha has with that version code. Nothing is cloned or built: it checks out only `Gemfile` and `Gemfile.lock` |

Test on Alpha, then promote the same build. The tag's message is the release notes; to write several lines, pass it
through `-F -` and a heredoc:

```bash
git tag -a v2.0.1-6-alpha -F - <<'EOF'
Smoother schedule and bookmarks.

• Backup and restore your bookmarks
• Faster show details
EOF
```

```bash
git push origin v2.0.1-6-alpha
```

Once it's tested, promote that same build to production with the same version name and code. Nothing is rebuilt, and
no message is needed: the release keeps its Alpha notes.

```bash
git tag v2.0.1-6-production
```

```bash
git push origin v2.0.1-6-production
```

- An `-alpha`, `-beta` or `-internal` tag's message becomes the release notes (en-US, at most 500 characters), line
  breaks included. Without a message, the notes are "Bug fixes and improvements." A production release keeps the notes
  it had on Alpha.
- The tag goes on the commit you're on. To tag another one, put its hash after the tag name
  (`git tag -a v2.0.1-6-alpha <commit> -F - <<'EOF'`).
- Check the message before pushing with `git tag -n20 v2.0.1-6-alpha`. Sourcetree's tag dialog only takes one line, so
  create multiline tags from its **Terminal**.
- Each version code can be uploaded to Play only once, so every new build needs a higher code.
- A production tag fails, changing nothing, if Alpha doesn't have that version code.

Setup, secrets and troubleshooting: [docs/8-play-release.md](docs/8-play-release.md).

## Project structure

```text
app/src/main/java/com/zc/bakamitai/
├── app/          # Activity, Application, root navigation, DI assembly
├── common/       # Code shared by two or more features (data, domain, di, ui)
└── feature/
    └── [name]/   # One package per feature: data, domain, di, presentation
```

This is the target layout for the rewrite. Placement rules are in [`.claude/skills/structure`](.claude/skills/structure/SKILL.md).

## Contributing

Architecture, naming, library and testing rules live in [`.claude/skills/`](.claude/skills/). [`CLAUDE.md`](CLAUDE.md) (also available as `AGENTS.md`) summarizes them for coding agents.

## License

Licensed under the [Apache License 2.0](LICENSE).
