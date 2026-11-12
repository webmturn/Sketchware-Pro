<p align="center">
  <img src="assets/Sketchware-Pro.png" style="width: 30%;" />
</p>

# Sketchware Pro
[![GitHub last commit](https://img.shields.io/github/last-commit/webmturn/Sketchware-Pro)](https://github.com/webmturn/Sketchware-Pro/commits/)
[![Repository Size](https://img.shields.io/github/repo-size/webmturn/Sketchware-Pro)](https://github.com/webmturn/Sketchware-Pro)
[![Upstream](https://img.shields.io/badge/upstream-Sketchware--Pro-blue)](https://github.com/Sketchware-Pro/Sketchware-Pro)

Welcome to Sketchware Pro! Here you'll find the source code of many classes in Sketchware Pro and, most importantly, the place to contribute to Sketchware Pro.

## Building the App

### Prerequisites
- **Android Studio** (recommended) or any IDE with Gradle support
- **JDK 17** or higher
- **Android SDK** with compileSdk 36

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/webmturn/Sketchware-Pro.git
   ```
2. Open the project in Android Studio.
3. Wait for Gradle sync to complete. The project does not apply the `google-services` Gradle plugin, so no Firebase configuration file is needed to build.
4. Build and run the `app` module on a device or emulator (minSdk 26).

> [!NOTE]
> The project uses **Gradle 8.13** with **AGP 8.12.0** and **Kotlin 2.1.21**. Java source/target compatibility is set to **Java 17**.

### Source Code Map

| Class | Package | Role |
| --- | --- | --- |
| `ProjectBuilder` | `pro.sketchware.core.build` | Runs the full project build pipeline (resources, Java, DEX, signing) |
| `ProjectFilePaths` | `pro.sketchware.core.build` | Resolves generated and build file paths for a project |
| `ManifestGenerator` | `pro.sketchware.core.codegen` | Generates `AndroidManifest.xml` |
| `ActivityCodeGenerator` | `pro.sketchware.core.codegen` | Generates activity Java source |
| `ComponentCodeGenerator` | `pro.sketchware.core.codegen` | Generates component and listener source |
| `EventCodeGenerator` | `pro.sketchware.core.codegen` | Generates event-handler source |
| `LayoutGenerator` | `pro.sketchware.core.codegen` | Generates layout XML |
| `GradleFileGenerator` | `pro.sketchware.core.codegen` | Generates Gradle build files |
| `BlockInterpreter` | `pro.sketchware.core.codegen` | Interprets block specs into Java code |
| `BlockCodeRegistry` | `pro.sketchware.core.codegen` | Registry-based block code generation |
| `ListenerCodeRegistry` | `pro.sketchware.core.codegen` | Listener code-generation patterns |
| `EventRegistry` | `pro.sketchware.core.codegen` | Known event names and configurations |
| `BlockSpecRegistry` | `pro.sketchware.core.block` | Block/event opCode specifications |
| `BuiltInLibrary` | `pro.sketchware.core.library` | Built-in library metadata |
| `LibraryManager` | `pro.sketchware.core.project` | External libraries and dependencies |
| `ResourceManager` | `pro.sketchware.core.project` | Project resources (images, colors, strings) |
| `ProjectDataManager` | `pro.sketchware.core.project` | Project-scoped manager factory |
| `ProjectFileManager` | `pro.sketchware.core.project` | Project persistence and loading |
| `ProjectListManager` | `pro.sketchware.core.project` | Project list and metadata |

> `:resolver` (Kotlin) resolves Maven/Jitpack dependencies for the library downloader;
> `:vendor-dx` is the bundled dx fork that `ProjectBuilder` uses to read and merge DEX files.

> [!TIP]
> Start from `pro.sketchware.core` for build/codegen work and `pro.sketchware.activities` for UI work. The old `mod` and `com.besome.sketch` trees are retired; `checkPackageBoundaries` rejects new files there.

### Documentation

- [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) — development handbook: architecture, code generation, build pipeline, and extension guides.
- [`docs/WIRELESS-DEBUGGING-zh.md`](docs/WIRELESS-DEBUGGING-zh.md) — wireless ADB debugging setup.
- [`docs/I18N-COVERAGE-zh.md`](docs/I18N-COVERAGE-zh.md) — Chinese localization coverage.

## Contributing

If you'd like to contribute to Sketchware Pro, follow these steps:

1. Fork this repository.
2. Make changes in your forked repository.
3. Test out those changes.
4. Create a pull request in this repository.
5. Your pull request will be reviewed by the repository members and merged if accepted.

We welcome contributions of any size, whether they are major features or bug fixes, but please note that all contributions will be thoroughly reviewed.

### Commit Message

When you make changes to one or more files, you need to commit those changes with a commit message. Here are some guidelines:

- Keep the commit message short and detailed.
- Use one of these commit types as a prefix:
  - `feat:` for a feature, possibly improving something already existing.
  - `fix:` for a fix, such as a bug fix.
  - `style:` for features and updates related to styling.
  - `refactor:` for refactoring a specific section of the codebase.
  - `test:` for everything related to testing.
  - `docs:` for everything related to documentation.
  - `chore:` for code maintenance (you can also use emojis to represent commit types).

Examples:
- `feat: Speed up compiling with new technique`
- `fix: Fix crash during launch on certain phones`
- `refactor: Reformat code in File.java`

> [!IMPORTANT]
> If you want to add new features that don't require editing other packages other than `pro.sketchware`, make your changes in `pro.sketchware` package, and respect the directories and files structure and names. Also, even though the project compiles just fine with Kotlin classes that you might add, try to make your changes or additions in Java, not Kotlin unless it is more than necessary.
>
> The hard rules for where new `.java` files must live are enforced by the `checkPackageBoundaries` Gradle task, which runs in CI.

## Thanks for Contributing

Thank you for contributing to Sketchware Pro! Your contributions help keep Sketchware Pro alive. Each accepted contribution will be noted down in the "About Team" activity. We'll use your GitHub name and profile picture initially, but they can be changed, of course.

## Disclaimer

This mod was not created for any harmful purposes, such as harming Sketchware; quite the opposite, actually. It was made to keep Sketchware alive by the community for the community. Please use it at your own discretion and consider becoming a Patreon backer to support the developers. Unfortunately, other ways to support them are not working anymore, so Patreon is the only available option currently. You can find their Patreon page [here](https://www.patreon.com/sketchware).

We do NOT permit publishing Sketchware Pro as it is, or with modifications, on Play Store or on any other app store. Keep in mind that this project is still a mod. Unauthorized modding of apps is considered illegal and we discourage such behavior.

We love Sketchware very much and are grateful to Sketchware's developers for creating such an amazing app. However, we haven't received updates for a long time. That's why we decided to keep Sketchware alive by creating this mod, and it's completely free. We don't demand any money :)
