# FinTrack - Personal Finance & Real-Time Budget Alerts

**FinTrack** is an Android personal finance management application built with Kotlin, Jetpack Compose, Material Design 3, and Firebase Firestore.

## Features

- **Live Financial Dashboard**:
  - Real-time balance calculations (Total Balance, Total Income, Total Expenses).
  - Monthly savings rate overview.
  - Recent transactions list.

- **Proactive Budget Alerts**:
  - Set custom monthly limits for individual categories (Food & Dining, Shopping, Transportation, Utilities, Entertainment, etc.).
  - Configurable alert thresholds (e.g. 70%, 80%, 90%).
  - Real-time warning banners and color-coded status badges (Green = Safe, Amber = Approaching Limit, Red = Over Budget).

- **Transaction Management**:
  - Track both Expenses and Income.
  - Categorization with contextual Material icons.
  - Search by description or merchant, and filter by transaction type or category.
  - Edit and delete transactions.

- **Savings Goals**:
  - Create and track custom savings goals (e.g. Emergency Fund, Travel, Gadgets).
  - Visual progress bars with percentage completion.
  - Quick "+ Deposit" funds modal.

- **Cloud Sync & Authentication**:
  - Integrated with **Firebase Firestore** using reactive snapshot flows.
  - Authenticated via **Google Sign-In** with Android Jetpack `CredentialManager`.
  - Zero-trust security rules with multi-user isolation under `/users/{userId}/`.

## Tech Stack & Architecture

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM with Clean Repository pattern & Kotlin StateFlow
- **Backend / Database**: Firebase Firestore with real-time snapshots
- **Authentication**: Jetpack Credential Manager (Google Sign-In)
- **Testing**: Robolectric JVM unit tests & Firebase Rules Unit Testing (`@firebase/rules-unit-testing`)
- **Build System**: Gradle Kotlin DSL (`.gradle.kts`)

## How to Build & Run

1. Open project in Android Studio (Giraffe or newer).
2. Connect an Android device or start an Android Emulator (API 24+).
3. Build and install:
   ```bash
   gradle assembleDebug
   ```
4. Run tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
