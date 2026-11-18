<div align="center">

# 💧 Smart Water Monitor

### A beautiful, feature-rich desktop app to track, analyse, and reduce your household water consumption.

![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue?style=for-the-badge&logo=java&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-3.45-lightblue?style=for-the-badge&logo=sqlite&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9.15-red?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

</div>

---

## ✨ Features

| 🗂️ Category | 🚀 Capability |
|---|---|
| 📊 **Dashboard** | Live usage stats, streak counter, animated progress bar |
| 📅 **Calendar View** | Colour-coded heatmap of daily usage vs limit |
| 📈 **Analytics** | 7-day trend chart, monthly bar chart, 30-day forecasting |
| 💡 **Insights** | AI-style tips on saving potential and usage patterns |
| 🗃️ **Records** | Full CRUD table with date, liters, status badges |
| 🔔 **Alerts** | Auto-triggered warnings at 70% and 90% of daily limit |
| 💸 **Billing** | Tariff plans with fixed charge + per-litre rate |
| 🔒 **Multi-User Auth** | Secure PBKDF2-SHA256 login, per-user data isolation |
| 📤 **Export** | CSV, Excel (Apache POI), and PDF (iText7) exports |
| 💾 **Backup** | One-click DB backup to a timestamped file |
| 🌙 **Themes** | Light and Dark mode with smooth transitions |
| 🌊 **Liquid Glass UI** | Physics-based spring animations and aurora backdrop |

---

## 🖼️ App Structure

```
💧 Smart Water Monitor
├── 🏠 Dashboard       ← Today's usage, streak, progress bar
├── 📅 Calendar        ← Monthly heatmap + summary stats
├── 📈 Analytics       ← Charts, leak detection, bill forecast
├── 💡 Insights        ← Readable tips & conservation advice
├── 🗃️ Records          ← Full usage history table
└── 📋 Request         ← Apply for a daily limit increase
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| 🖥️ **UI Framework** | JavaFX 21 with custom CSS themes |
| 🗄️ **Database** | SQLite via `sqlite-jdbc` |
| 🔐 **Auth** | PBKDF2WithHmacSHA256 + SecureRandom salt |
| 📊 **Analytics** | Custom trend & prediction engine |
| 🗃️ **Export** | Apache Commons CSV · Apache POI · iText7 |
| ⚡ **Cache** | Caffeine in-memory cache |
| ✅ **Validation** | Jakarta Bean Validation + Hibernate Validator |
| 📝 **Logging** | SLF4J + Logback |
| 📦 **Build** | Apache Maven 3.9.15 (bundled) |

---

## 🚀 Getting Started

### Prerequisites

- ☕ **JDK 17 or higher** — [Download here](https://adoptium.net/)
- 📦 **Maven 3.6+** — [Download here](https://maven.apache.org/download.cgi) or use the bundled copy in `apache-maven-3.9.15/`

### ▶️ Run the App

```powershell
# Clone or extract the project
cd e:\WSR

# Build & launch (downloads dependencies on first run)
.\apache-maven-3.9.15\bin\mvn.cmd javafx:run
```

> 💡 On first launch the app will show a **Setup Wizard** to create your profile and first user account.

### 🧪 Run Tests

```powershell
.\apache-maven-3.9.15\bin\mvn.cmd test
```

### 📦 Build a JAR

```powershell
.\apache-maven-3.9.15\bin\mvn.cmd package
```

---

## 📁 Project Layout

```
e:\WSR
├── 📄 pom.xml                        ← Maven build config
├── 📂 src/main/java/com/smartwater/
│   ├── 🚀 MainApp.java               ← Entry point
│   ├── 🎮 controller/                ← WaterController (business logic)
│   ├── 🗄️  database/                  ← DatabaseConnection, DatabaseSchema
│   ├── 🔐 auth/                      ← AuthenticationService, UserManager
│   ├── 📊 analytics/                 ← AnalyticsService, BillingService, LeakDetection
│   ├── 🏠 model/                     ← WaterRecord, User, HouseholdProfile, Meter…
│   ├── 🖥️  ui/                        ← DashboardView, AuthView, ThemeManager, UiMotion
│   │   └── 📑 tabs/                  ← DashboardTab
│   ├── 💾 storage/                   ← ProfileManager, MeterManager, AlertManager…
│   ├── 📤 export/                    ← ExportService, ReportGenerator
│   ├── 🔔 notification/              ← NotificationService
│   ├── 📋 policy/                    ← MunicipalityPolicy
│   ├── ⚙️  settings/                  ← SettingsManager
│   └── 🛡️  validation/               ← InputValidator, ValidationUtil
├── 📂 src/main/resources/
│   ├── 🎨 style.css                  ← Main stylesheet
│   ├── 🎨 auth-styles.css
│   └── 🎨 styles/dark-theme.css · light-theme.css
├── 📂 src/test/                      ← JUnit 5 tests
├── 📂 data/                          ← SQLite DB + backup files
└── 📂 logs/                          ← Logback output
```

---

## 🔐 Default Accounts

> Accounts are created by you during first run. Passwords are hashed with **PBKDF2-SHA256** and never stored in plain text.

To reset a password, use any SQLite browser or run:

```powershell
# Generate a new hash and update the DB
# (see AuthenticationService.hashPassword for the algorithm)
```

---

## 📊 Data & Analytics

The analytics engine provides:

- 📉 **Trend Analysis** — compares last 30 days to identify INCREASING / DECREASING / STABLE patterns
- 🔮 **Next-Month Forecast** — simple linear regression on recent usage
- 💧 **Leak Detection** — flags abnormal overnight or consecutive-day spikes
- 💸 **Bill Estimation** — real-time bill calculation using your tariff plan
- 🏆 **Conservation Streak** — counts consecutive days under the daily limit

---

## 🎨 UI & Animations

The app uses a custom physics engine (`UiMotion`) with:

- 🌊 **Spring physics** — hover-lift on every card, natural settle curves
- 🌈 **Aurora backdrop** — floating gradient blobs (optimised, 30fps-capped parallax)
- ✨ **Liquid Glass cards** — specular shimmer that follows your cursor
- 🔄 **Stagger reveal** — tab content fades in with cascading delays
- 🌙 **Instant theme switching** — Light ↔ Dark with no restart needed

---

## 🗺️ Roadmap

- [ ] 🌐 Cloud sync / multi-device support
- [ ] 📱 Mobile companion app
- [ ] 🤖 ML-based anomaly detection
- [ ] 🏢 Multi-household / admin dashboard
- [ ] 📡 Smart meter hardware integration

---

## 🤝 Contributing

Pull requests are welcome! Please open an issue first to discuss major changes.

1. 🍴 Fork the repo
2. 🌿 Create a feature branch (`git checkout -b feature/my-feature`)
3. ✅ Commit with clear messages
4. 📬 Open a Pull Request

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with 💙 and ☕ · © 2026 Smart Water Monitor

</div>
