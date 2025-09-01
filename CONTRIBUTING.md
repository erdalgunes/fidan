# Contributing to Fidan 🌱

First off, thank you for considering contributing to Fidan! It's people like you who help make Fidan a tool that truly helps both productivity and our planet.

## 🌍 Our Values

Before contributing, please understand that Fidan is built on these core values:
- **Environmental Impact**: Every feature should align with our mission to plant real trees
- **Ethical Use**: We use the Hippocratic License to ensure ethical usage
- **Accessibility**: The app should be usable by everyone
- **Privacy First**: We don't track users unnecessarily
- **Open Source**: Transparency in everything we do

## 🤝 Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md). We're committed to providing a welcoming and inclusive experience for everyone.

## 🚀 Getting Started

### Prerequisites

For Android development:
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK with API level 21+

For Garmin development:
- Visual Studio Code with Monkey C extension
- Garmin Connect IQ SDK
- Compatible Garmin device or simulator

### Setting Up Your Development Environment

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR-USERNAME/fidan.git
   cd fidan
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/erdalgunes/fidan.git
   ```

## 🌳 How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, please include:

- A clear and descriptive title
- Steps to reproduce the behavior
- Expected behavior
- Actual behavior
- Screenshots (if applicable)
- Your environment (OS, device, app version)

### Suggesting Features

Feature suggestions are welcome! Please:

- Check if the feature has already been suggested
- Provide a clear use case
- Explain how it aligns with Fidan's mission
- Consider if it could be implemented in a privacy-preserving way

### Pull Requests

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes following our coding standards:
   - **Kotlin (Android)**: Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
   - **Monkey C (Garmin)**: Follow consistent formatting with existing code
   - Write clear, self-documenting code
   - Add comments only when necessary
   - Write unit tests for new features

3. Commit your changes:
   ```bash
   git commit -m "Add feature: Brief description"
   ```
   - Use present tense ("Add feature" not "Added feature")
   - Keep commits atomic and focused
   - Reference issues and pull requests when relevant

4. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

5. Open a Pull Request with:
   - A clear title and description
   - Link to any related issues
   - Screenshots or GIFs for UI changes
   - Confirmation that you've tested your changes

## 🧪 Testing

- Run existing tests before submitting PR
- Add tests for new features
- Ensure all tests pass
- Test on real devices when possible

### Android Testing
```bash
cd android-app
./gradlew test
./gradlew connectedAndroidTest
```

### Garmin Testing
Test using the Connect IQ simulator with various device profiles.

## 📚 Documentation

- Update README.md if needed
- Document new features
- Add inline documentation for complex logic
- Update API documentation for public methods

## 🎨 Design Guidelines

- Follow Material Design guidelines for Android
- Keep UI simple and focused
- Ensure accessibility (proper contrast, screen reader support)
- Test with different screen sizes
- Keep the tree-growing theme consistent

## 🌱 Areas We Need Help

- **Translations**: Help make Fidan available in more languages
- **Tree Graphics**: Create beautiful tree species artwork
- **Testing**: Test on various devices and report issues
- **Documentation**: Improve docs and create tutorials
- **Performance**: Optimize battery usage and app performance
- **Accessibility**: Improve screen reader support and accessibility features

## 💬 Communication

- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For questions and community discussions
- **Pull Request Comments**: For code-specific discussions

## 🎯 Priorities

Current priorities (check our project board for latest):
1. Core Pomodoro functionality
2. Basic tree growing mechanics
3. Garmin app development
4. Statistics and progress tracking

## 📜 License

By contributing to Fidan, you agree that your contributions will be licensed under the Hippocratic License 3.0.

## 🙏 Recognition

Contributors will be:
- Listed in our CONTRIBUTORS.md file
- Mentioned in release notes
- Given credit in the app's about section

## ❓ Questions?

Feel free to open an issue with the "question" label or start a discussion.

---

Thank you for helping us grow forests while staying focused! 🌳✨