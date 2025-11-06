# Contributing to SmartFind

Thank you for your interest in contributing to SmartFind! This document provides guidelines for contributing to the project.

---

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Respect differing viewpoints and experiences

---

## How to Contribute

### Reporting Bugs

1. **Search existing issues** to avoid duplicates
2. **Create a new issue** with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Device details (model, Android version)
   - Screenshots/logs if applicable

### Suggesting Features

1. **Check existing feature requests** first
2. **Open a new issue** tagged as "enhancement"
3. **Describe the feature:**
   - Use case and motivation
   - Proposed implementation (if technical)
   - Alternative solutions considered

### Submitting Code

1. **Fork the repository**
2. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes:**
   - Follow Kotlin coding conventions
   - Add comments for complex logic
   - Update documentation if needed
4. **Test your changes:**
   - Run on multiple devices
   - Check for memory leaks (LeakCanary)
   - Verify offline functionality
5. **Commit with clear messages:**
   ```bash
   git commit -m "Add feature: describe what you added"
   ```
6. **Push to your fork:**
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request:**
   - Link related issues
   - Describe changes clearly
   - Include screenshots for UI changes

---

## Development Setup

See [README.md](README.md) for setup instructions.

---

## Coding Standards

### Kotlin Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable names
- Keep functions small and focused
- Maximum line length: 120 characters

### Architecture

- **MVVM + Clean Architecture**
- **Repository pattern** for data access
- **Coroutines** for async operations (no blocking calls on main thread)
- **LiveData** for UI state management

### Testing

- Write unit tests for business logic
- Test edge cases (permissions denied, no internet, storage full)
- Verify memory leak fixes with LeakCanary

### Commit Messages

- Use present tense: "Add feature" not "Added feature"
- Be descriptive: "Fix crash when camera unavailable" not "Fix bug"
- Reference issues: "Closes #123"

---

## Pull Request Checklist

- [ ] Code follows project style guidelines
- [ ] No new compiler warnings
- [ ] Tested on physical devices (min 2)
- [ ] No memory leaks (LeakCanary clean)
- [ ] Documentation updated (if applicable)
- [ ] Screenshots included (for UI changes)
- [ ] Commit messages are clear

---

## License

By contributing, you agree that your contributions will be licensed under the Apache 2.0 License.

---

## Questions?

Open an issue or reach out via [contact method].

Thank you for contributing! 🎉
