## 🐛 Bug Reports

Please include:
- OS and Java version (`java -version`)
- Steps to reproduce
- What you expected vs what happened
- Logs from `logs/smartwater.log`

## ✨ Feature Requests

Open an issue with the `enhancement` label describing:
- The use case
- What problem it solves

## 🔧 Pull Requests

1. Fork the repo and create a branch: `git checkout -b feature/my-feature`
2. Make your changes with clear, focused commits
3. Run tests: `.\apache-maven-3.9.15\bin\mvn.cmd test`
4. Open a PR against `main` — describe what changed and why

### Code Style
- Follow existing Java naming conventions
- Keep UI code in `com.smartwater.ui`, business logic in `controller/analytics`
- Add SLF4J logging for any new service methods
- Write a JUnit 5 test for new analytical/business logic

Thank you for contributing! 💙
