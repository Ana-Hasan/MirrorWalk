# 🏃 MirrorWalk

### Race Your Past. Run Your Future.

MirrorWalk is a native Android run/walk tracking application that combines **GPS-based fitness tracking, augmented reality, and social interaction** into one platform.

Unlike traditional running apps that primarily focus on numbers such as distance and pace, MirrorWalk introduces an **AR Ghost Runner** that allows users to race against a visual replay of a previous run directly in the real world.

---

## ✨ What is MirrorWalk?

Most running apps answer one question:

> **"How did I perform?"**

MirrorWalk asks another:

> **"Can I beat my past self?"**

The application tracks a user's run or walk using GPS, filters unreliable location data, calculates live performance metrics, and stores completed runs locally.

The standout feature is **Ghost Mode**. A previously recorded run can be replayed as a translucent virtual runner through the phone's camera using ARCore. The ghost follows the original run's timing, allowing the current runner to see whether they are ahead of or behind their previous performance.

MirrorWalk is designed around three core areas:

* 📍 **Run & Walk Tracking**
* 👻 **AR Ghost Runner**
* 👥 **Social Running Experience**

---

## 🚀 Key Features

### 📍 GPS Run & Walk Tracking

MirrorWalk continuously tracks the user's location during an active session.

* Start a run/walk session
* Pause and resume tracking
* Stop and save a session
* Receive continuous GPS coordinates
* Record latitude, longitude, and timestamps
* Filter noisy and unrealistic GPS readings
* Calculate live distance
* Calculate live pace
* Track elapsed time
* Draw the route on a map in real time

GPS filtering helps prevent inaccurate readings from creating unrealistic jumps in distance or pace.

---

### 🗺️ Live Route Mapping

The user's route is displayed as it is being recorded.

MirrorWalk uses:

* **MapLibre Native Android SDK**
* **OpenFreeMap tiles**

This avoids the need for a Google Maps API key or billing account.

After a run is completed, the full route can be viewed again from the user's run history.

---

### 💾 Local Run History

Completed runs are stored locally using **Room Database**.

Each saved run contains information such as:

* Run ID
* Date
* Distance
* Duration
* Average pace
* GPS route points

Users can:

* View previous runs
* Open a previous route on the map
* Review run statistics
* Delete saved runs

Because the run history is stored locally, it remains available even without an internet connection.

---

### 👻 AR Ghost Runner

The defining feature of MirrorWalk.

Ghost Mode allows users to race against a previous performance using augmented reality.

The workflow is:

```text
Select Previous Run
        ↓
Open Ghost Mode
        ↓
Initialize ARCore
        ↓
Detect Ground Plane
        ↓
Place Ghost Runner
        ↓
Replay Previous Run
        ↓
Compare Live Performance
```

The ghost runner is rendered as a translucent 3D model and anchored to a detected real-world surface.

Most importantly, the ghost does **not** simply move at a constant speed.

Its movement is based on the **timestamps from the original recorded run**, allowing it to reproduce the pace changes of the original performance.

The user can see their live distance and pace alongside the ghost's corresponding performance.

---

## 🧠 How GPS Tracking Works

Raw GPS data can contain inaccuracies caused by:

* Poor GPS signal
* Tall buildings
* Location jitter
* Unrealistic location jumps
* Low reported location accuracy

MirrorWalk therefore processes incoming GPS points before using them.

```text
GPS Location Update
        ↓
Extract latitude / longitude / timestamp
        ↓
Check location accuracy
        ↓
Check realistic movement speed
        ↓
Check for GPS jitter
        ↓
       Valid?
      /     \
    No       Yes
    ↓         ↓
 Discard   Store Point
              ↓
      Calculate Distance
              ↓
        Calculate Pace
              ↓
       Update Live UI
              ↓
         Draw Route
```

This keeps the recorded run more reliable than simply accumulating every raw GPS reading.

---

## 🏗️ System Architecture

MirrorWalk follows a layered architecture designed around modern Android development.

```text
┌─────────────────────────────────────┐
│           UI Layer                  │
│       Jetpack Compose               │
├─────────────────────────────────────┤
│         ViewModel Layer             │
│       StateFlow + ViewModel         │
├─────────────────────────────────────┤
│          Data / Logic Layer         │
│ GPS Tracking │ GPS Filter │ Metrics │
├─────────────────────────────────────┤
│           Storage Layer             │
│             Room DB                 │
├─────────────────────────────────────┤
│        Rendering / AR Layer         │
│ MapLibre │ ARCore │ SceneView       │
└─────────────────────────────────────┘
```

The separation between tracking logic and presentation allows GPS filtering and distance/pace calculations to be tested independently of the UI.

---

## 🛠️ Tech Stack

| Technology                            | Purpose                      |
| ------------------------------------- | ---------------------------- |
| **Kotlin 2.4.x**                      | Primary programming language |
| **Android Studio Quail 4 / 2026.1.4** | Development environment      |
| **Gradle 9.6**                        | Build system                 |
| **AGP 9.4.0**                         | Android build tooling        |
| **JDK 17**                            | Java runtime                 |
| **Jetpack Compose**                   | UI development               |
| **Material 3**                        | UI components                |
| **Navigation Compose**                | App navigation               |
| **ViewModel**                         | UI state and business logic  |
| **Kotlin Coroutines**                 | Asynchronous operations      |
| **StateFlow**                         | Reactive state management    |
| **Fused Location Provider**           | GPS/location tracking        |
| **MapLibre Native Android SDK**       | Map rendering                |
| **OpenFreeMap**                       | Map tiles                    |
| **Room 2.8.5**                        | Local data persistence       |
| **Google ARCore 1.56.0**              | Augmented reality            |
| **SceneView AR 4.34.0**               | AR rendering                 |
| **Filament**                          | 3D rendering                 |

---

## 📱 Application Workflow

### Standard Run

```text
Launch App
    ↓
Run Screen
    ↓
Request Location Permission
    ↓
Permission Granted?
    ↓
Start Run
    ↓
Receive GPS Updates
    ↓
Filter GPS Points
    ↓
Calculate Distance / Pace / Time
    ↓
Update UI + Draw Route
    ↓
Pause / Resume
    ↓
Stop Run
    ↓
Show Run Summary
    ↓
Save to Room Database
    ↓
View Run History
```

### Ghost Run

```text
Select Ghost Run
       ↓
Open AR Screen
       ↓
Initialize ARCore
       ↓
Detect Ground Plane
       ↓
Anchor Ghost Runner
       ↓
Load Recorded Route
       ↓
Replay Using Original Timestamps
       ↓
Display Ghost + Live Statistics
       ↓
Compare Performance
```

---

## 🗃️ Data Model

A completed run consists of the run itself and its associated GPS points.

```text
Run
├── runId
├── userId
├── date
├── distance
├── duration
├── averagePace
└── routePoints
       │
       ├── latitude
       ├── longitude
       └── timestamp
```

Conceptually:

```text
User
  │
  │ creates
  ▼
Run
  │
  │ contains
  ▼
GPSPoint
  ├── latitude
  ├── longitude
  └── timestamp
```

---

## 🧪 Testing

Because MirrorWalk is currently a prototype, testing has focused primarily on the tracking engine and AR functionality.

### Unit Testing

Individual components were tested using sample GPS data, including deliberately noisy points.

Tested areas include:

* Distance calculation
* Pace calculation
* GPS speed filtering
* GPS accuracy filtering
* Jitter detection

### Integration Testing

The complete tracking flow was tested together:

```text
Start
 → GPS Updates
 → Filtering
 → Live Metrics
 → Map Drawing
 → Pause
 → Resume
 → Stop
 → Save
```

This verifies synchronization between the UI, ViewModel, and location tracking system.

### Performance Testing

Real outdoor walks and runs were used to evaluate:

* Smooth metric updates
* Tracking responsiveness
* Noticeable lag
* Battery impact

### AR Testing

The AR Ghost Runner was tested on indoor and outdoor flat surfaces to evaluate:

* Plane detection
* Ground anchoring
* 3D model placement
* Timestamp-based ghost movement

---

## 📊 Current Prototype Status

| Feature                        | Status                   |
| ------------------------------ | ------------------------ |
| GPS point filtering            | ✅ Working                |
| Live distance calculation      | ✅ Working                |
| Live pace calculation          | ✅ Working                |
| Route drawing                  | ✅ Working                |
| Start / Pause / Resume / Stop  | ✅ Working                |
| Room-based run storage         | ✅ Working                |
| Run history                    | ✅ Working                |
| Saved route viewing            | ✅ Working                |
| Run deletion                   | ✅ Working                |
| AR plane detection             | ✅ Working                |
| AR ground anchoring            | ✅ Working                |
| Timestamp-based ghost movement | ✅ Working                |
| Live vs Ghost comparison       | ✅ Prototype              |
| Real saved-run Ghost Mode      | 🔄 Next integration step |
| User profiles                  | 🔜 Planned               |
| Followers / Following          | 🔜 Planned               |
| Social feed                    | 🔜 Planned               |
| Run posts with photos          | 🔜 Planned               |
| Fitness analysis               | 🔜 Planned               |

> **Current AR limitation:** The prototype Ghost Runner currently uses hardcoded sample route data. The data structure is designed so that it can later be replaced with an actual saved run.

---

## 🎯 Project Objectives

MirrorWalk is designed around three main objectives:

### 1. Reliable Fitness Tracking

Provide accurate and trustworthy run/walk records through GPS tracking, filtering, distance calculation, pace calculation, and route storage.

### 2. Social Running

Create a running-focused social experience with profiles, followers, following, and shared runs.

### 3. AR-Based Motivation

Use augmented reality to turn a previous performance into a visual opponent that users can race against.

---

## 🔮 Future Scope

The next stage of MirrorWalk will expand the social and analytical aspects of the application.

Planned features include:

* 👤 User profiles
* 👥 Followers and following
* 📱 Running-focused social feed
* 📸 Run posts with optional photos
* 📈 Deeper fitness analysis
* 🏃 Ghost races using actual saved runs
* 🏆 Performance-based challenges

The long-term goal is to move beyond simply recording a run and create an experience that makes users want to return and improve.

---

## 🎨 Design

MirrorWalk uses a dark navy and charcoal visual identity with mint and lime accents.

The interface is built around:

* Rounded content cards
* Bottom navigation
* Central run action button
* Modern typography
* High-contrast fitness statistics
* Camera-first AR interactions

The primary font is **Manrope**, with **Space Grotesk Bold** used for prominent headings and subtitles.

---

## 📂 Project Structure

A conceptual organization of the application is:

```text
MirrorWalk/
│
├── ui/
│   ├── screens/
│   │   ├── RunScreen
│   │   ├── MapScreen
│   │   ├── RunHistoryScreen
│   │   └── ARGhostRunScreen
│   │
│   └── components/
│
├── viewmodel/
│   ├── RunViewModel
│   ├── HistoryViewModel
│   └── ARGhostViewModel
│
├── tracking/
│   ├── LocationTracker
│   ├── GPSFilter
│   └── RunCalculator
│
├── ar/
│   ├── ARManager
│   └── GhostRunner
│
├── data/
│   ├── Run
│   ├── GPSPoint
│   ├── RunDao
│   └── AppDatabase
│
└── navigation/
```

---

## ⚙️ Getting Started

### Prerequisites

Make sure you have:

* Android Studio
* JDK 17
* Android SDK
* A compatible Android device or emulator
* Git

For AR functionality, the device must support **Google ARCore**.

### Clone the Repository

```bash
git clone <repository-url>
cd MirrorWalk
```

### Open in Android Studio

1. Open Android Studio.
2. Select **Open**.
3. Select the MirrorWalk project directory.
4. Allow Gradle to sync.
5. Build the project.
6. Run the application on a compatible Android device.

### Location Permissions

The application requires location permissions to perform run tracking.

### AR Permissions

Ghost Mode requires camera access and an ARCore-compatible device.

---

## 🧩 Core Architecture

```text
                 ┌───────────────────┐
                 │    Jetpack        │
                 │     Compose       │
                 └─────────┬─────────┘
                           │
                           ▼
                 ┌───────────────────┐
                 │    ViewModels     │
                 │   StateFlow       │
                 └───────┬───┬───────┘
                         │   │
              ┌──────────┘   └──────────┐
              ▼                         ▼
     ┌─────────────────┐       ┌─────────────────┐
     │ Tracking Engine │       │   AR Module     │
     │                 │       │                 │
     │ GPS             │       │ ARCore          │
     │ Filtering       │       │ SceneView       │
     │ Distance        │       │ Filament        │
     │ Pace            │       │ Ghost Runner    │
     └────────┬────────┘       └─────────────────┘
              │
              ▼
     ┌─────────────────┐
     │   Room Database │
     │                 │
     │ Saved Runs      │
     │ GPS Points      │
     └─────────────────┘
```

---

## 🌟 Why MirrorWalk?

MirrorWalk brings together three experiences that are usually separated:

```text
             FITNESS
                │
                │
       ┌────────┴────────┐
       │                 │
     SOCIAL             AR
       │                 │
       └────────┬────────┘
                │
                ▼
           MIRRORWALK
```

It isn't just about knowing **how far you ran**.

It's about seeing your performance, sharing it, and having something tangible to chase.

### Race your past. Run your future. 🏃‍♀️👻

---

## 📄 Project Information

**Project:** MirrorWalk
**Platform:** Native Android
**Language:** Kotlin
**UI:** Jetpack Compose
**Project Stage:** Prototype / Design Project

Built as a native Android project exploring the combination of **fitness tracking, augmented reality, and social interaction**.
