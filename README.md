<p align="center">
  <img src="assets/Sketchware-Pro.png" style="width: 30%;" />
</p>

# Sketchware Pro
[![GitHub contributors](https://img.shields.io/github/contributors/Sketchware-Pro/Sketchware-Pro)](https://github.com/Sketchware-Pro/Sketchware-Pro/graphs/contributors)
[![GitHub last commit](https://img.shields.io/github/last-commit/Sketchware-Pro/Sketchware-Pro)](https://github.com/Sketchware-Pro/Sketchware-Pro/commits/)
[![Discord server stats](https://img.shields.io/discord/790686719753846785)](http://discord.gg/kq39yhT4rX)
[![Total downloads](https://img.shields.io/github/downloads/Sketchware-Pro/Sketchware-Pro/total)](https://github.com/Sketchware-Pro/Sketchware-Pro/releases)
[![Repository Size](https://img.shields.io/github/repo-size/Sketchware-Pro/Sketchware-Pro)](https://github.com/Sketchware-Pro/Sketchware-Pro)

Welcome to Sketchware Pro! Here you'll find the source code of many classes in Sketchware Pro and, most importantly, the place to contribute to Sketchware Pro.

## Building the App

### Prerequisites
- **Android Studio** (recommended) or any IDE with Gradle support
- **JDK 17** or higher
- **Android SDK** with compileSdk 36

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/Sketchware-Pro/Sketchware-Pro.git
   ```
2. Open the project in Android Studio.
3. Wait for Gradle sync to complete. If `google-services.json` is missing, a mock file is generated automatically during the pre-build step.
4. Build and run the `app` module on a device or emulator (minSdk 26).

> [!NOTE]
> The project uses **Gradle 8.13** with **AGP 8.12.0** and **Kotlin 2.1.21**. Java source/target compatibility is set to **Java 17**.

### Source Code Map

| Class           | Role                                        |
| --------------- | ------------------------------------------- |
| `pro.sketchware.core.ProjectBuilder` | Helper for compiling an entire project |
| `pro.sketchware.core.ManifestGenerator` | Responsible for generating AndroidManifest.xml |
| `pro.sketchware.core.ActivityCodeGenerator` | Generates source code of activities |
| `pro.sketchware.core.ComponentCodeGenerator` | Generates source code of components, such as listeners, etc. |
| `pro.sketchware.core.EventCodeGenerator` | Generates source code for event handlers |
| `pro.sketchware.core.LayoutGenerator` | Responsible for generating XML files of layouts |
| `pro.sketchware.core.GradleFileGenerator` | Generates Gradle build files |
| `pro.sketchware.core.BlockInterpreter` | Interprets and translates block specifications into Java code |
| `pro.sketchware.core.BlockCodeRegistry` | Registry-based replacement for block code interpretation |
| `pro.sketchware.core.BlockSpecRegistry` | Central registry mapping block/event opCodes to their specifications |
| `pro.sketchware.core.ListenerCodeRegistry` | Registry for listener code generation patterns |
| `pro.sketchware.core.EventRegistry` | Registry of known event names and configurations |
| `pro.sketchware.core.BuiltInLibrary` | Represents a single built-in library and its metadata |
| `pro.sketchware.core.LibraryManager` | Manages external libraries and dependencies |
| `pro.sketchware.core.ResourceManager` | Manages project resources (images, colors, strings, etc.) |
| `pro.sketchware.core.ProjectDataManager` | Singleton factory for project-scoped manager instances |
| `pro.sketchware.core.ProjectFileManager` | Manages project file persistence and loading |
| `pro.sketchware.core.ProjectListManager` | Manages the list of projects |
| `pro.sketchware.core.ProjectFilePaths` | Organizes Sketchware projects' file paths |
| `pro.sketchware.core.CompileQuizManager` | Responsible for the compiling dialog's quizzes |

> [!TIP]
> You can also check the `mod` package, which contains the majority of contributors' changes.

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

## Thanks for Contributing

Thank you for contributing to Sketchware Pro! Your contributions help keep Sketchware Pro alive. Each accepted contribution will be noted down in the "About Team" activity. We'll use your GitHub name and profile picture initially, but they can be changed, of course.

## Discord

Want to chat with us, discuss changes, or just hang out? We have a Discord server just for that.

[![Join our Discord server!](https://invidget.switchblade.xyz/kq39yhT4rX)](http://discord.gg/kq39yhT4rX)

## Disclaimer

This mod was not created for any harmful purposes, such as harming Sketchware; quite the opposite, actually. It was made to keep Sketchware alive by the community for the community. Please use it at your own discretion and consider becoming a Patreon backer to support the developers. Unfortunately, other ways to support them are not working anymore, so Patreon is the only available option currently. You can find their Patreon page [here](https://www.patreon.com/sketchware).

We do NOT permit publishing Sketchware Pro as it is, or with modifications, on Play Store or on any other app store. Keep in mind that this project is still a mod. Unauthorized modding of apps is considered illegal and we discourage such behavior.

We love Sketchware very much and are grateful to Sketchware's developers for creating such an amazing app. However, we haven't received updates for a long time. That's why we decided to keep Sketchware alive by creating this mod, and it's completely free. We don't demand any money :)
