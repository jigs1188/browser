# Mobile Extension Runtime (MER)

A lightweight, programmable browser runtime for Android that allows users to install and run mini-extensions inside WebViews — with built-in AI-assisted webpage augmentation.

**Think:** Tampermonkey for Android + AI-powered browsing.

**This is NOT a Chrome fork. This is NOT a full browser.** It's a WebView-based runtime with an extension engine and AI capabilities.

---

## Features

- 🌐 **Browser with Tabs** — WebView-based browser with tab management
- 🧩 **Extension System** — Install, enable/disable, and manage lightweight extensions
- 💉 **JS/CSS Injection** — Inject scripts and styles into any webpage
- 🔗 **Runtime Bridge** — Kotlin ↔ JavaScript communication via `mer.*` API
- 📦 **Manifest System** — JSON-based extension manifests with URL pattern matching
- 🔐 **Permission Model** — Extensions declare required permissions (storage, ai, etc.)
- 💾 **Storage API** — Namespaced key-value storage per extension
- 🎯 **URL Matching** — Extensions execute only on matching URLs
- 🤖 **AI Integration** — Gemini-powered summarize, explain, ask, extract APIs
- 🎨 **Overlay API** — Extensions can display floating UI panels on pages

---

## Architecture

```
┌──────────────────────────────────────────────────┐
│                  UI Layer (Compose)               │
│  BrowserScreen │ ExtensionList │ Settings │ AiUI  │
├──────────────────────────────────────────────────┤
│                ViewModel Layer                    │
│  BrowserViewModel │ ExtensionListViewModel        │
├──────────────────────────────────────────────────┤
│                 Runtime Layer                     │
│  ExtensionRuntime │ InjectionEngine │ UrlMatcher  │
│  RuntimeBridge │ AiBridge (Kotlin ↔ JS)           │
├──────────────────────────────────────────────────┤
│                   AI Layer                        │
│  GeminiClient │ AiService                         │
├──────────────────────────────────────────────────┤
│                Extension Layer                    │
│  ExtensionLoader │ ManifestParser │ Repository    │
├──────────────────────────────────────────────────┤
│                 Storage Layer                     │
│  Room DB │ ExtensionFileManager │ ExtensionStorage│
│  AppPreferences (EncryptedSharedPreferences)      │
└──────────────────────────────────────────────────┘
```

### Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room |
| Serialization | kotlinx-serialization |
| Browser Engine | Android WebView |
| Async | Coroutines + Flow |
| AI | Google Gemini free tier |
| Security | EncryptedSharedPreferences |

---

## Runtime API

Extensions have access to the `mer` global object:

```javascript
// Storage (requires "storage" permission)
mer.storage.get("key");           // Returns parsed value or null
mer.storage.set("key", value);    // Stores JSON-serialized value
mer.storage.remove("key");        // Removes a key

// Runtime info
mer.runtime.getId();              // Returns the extension ID
mer.runtime.hasPermission("ai");  // Check if permission is granted

// Logging (appears in Android Logcat)
mer.log.debug("message");
mer.log.info("message");
mer.log.warn("message");
mer.log.error("message");

// Overlay API — floating UI panels
mer.overlay.show({ title: "Hello", content: "World" });
mer.overlay.update(overlayId, { content: "Updated!" });
mer.overlay.dismiss(overlayId);

// AI API (requires "ai" permission) — all return Promises
mer.ai.ask("What is this about?");
mer.ai.summarize();                // Summarizes current page
mer.ai.explain("selected text");   // Explains text in simple terms
mer.ai.extract("find all emails"); // Extracts data from page
mer.ai.isAvailable();              // Check if AI is configured
```

---

## Extension Format

Extensions are folders containing a `manifest.json` and their script/style files:

```
my-extension/
├── manifest.json
├── content.js
└── styles.css
```

### Manifest Schema

```json
{
    "manifest_version": 1,
    "name": "My Extension",
    "version": "1.0.0",
    "description": "What it does",
    "matches": ["*://*.example.com/*"],
    "scripts": ["content.js"],
    "styles": ["styles.css"],
    "permissions": ["storage", "ai"],
    "run_at": "document_end"
}
```

### Available Permissions

| Permission | API Access |
|---|---|
| `storage` | `mer.storage.*` |
| `ai` | `mer.ai.*` |
| `notifications` | _(future)_ |
| `tabs` | _(future)_ |
| `clipboard` | _(future)_ |
| `network` | _(future)_ |

### URL Match Patterns

| Pattern | Matches |
|---|---|
| `*://*.reddit.com/*` | Any Reddit page, any scheme |
| `https://example.com/*` | Any HTTPS page on example.com |
| `*://*/*` | All pages |
| `<all_urls>` | All pages |

---

## AI Setup

MER uses Google Gemini free tier for AI features:

1. Get a free API key from [aistudio.google.com](https://aistudio.google.com)
2. Open MER → Menu → Settings
3. Enter your API key
4. The AI button (✨) appears in the URL bar
5. Extensions can now use `mer.ai.*` APIs

**Cost:** Free (Gemini free tier — 15+ requests/minute)

---

## Building

```bash
# Clone
git clone https://github.com/jigs1188/mobile-extension-runtime.git
cd mobile-extension-runtime

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

**Requirements:** Android Studio Ladybug+ / JDK 17+ / Android SDK 35

---

## Project Structure

```
app/src/main/java/dev/mer/
├── MerApplication.kt          # Hilt app entry point
├── MainActivity.kt            # Single activity host
├── ai/                         # AI integration
│   ├── GeminiClient.kt        # Gemini API HTTP client
│   └── AiService.kt           # High-level AI operations
├── ui/                         # Presentation layer
│   ├── browser/                # Browser screen + WebView + AI overlay
│   ├── extensions/             # Extension management
│   ├── settings/               # Settings (API key config)
│   ├── components/             # Shared UI components
│   ├── navigation/             # Nav graph
│   └── theme/                  # Design system
├── runtime/                    # Extension execution engine
│   ├── ExtensionRuntime.kt     # Orchestrator
│   ├── InjectionEngine.kt     # JS/CSS injection
│   ├── UrlMatcher.kt          # URL pattern matching
│   └── bridge/                 # Kotlin ↔ JS bridges
│       ├── RuntimeBridge.kt   # Storage, logging, runtime
│       └── AiBridge.kt        # AI operations (async)
├── extension/                  # Extension data layer
│   ├── model/                  # Domain models
│   ├── parser/                 # Manifest parsing
│   ├── loader/                 # Installation pipeline
│   └── repository/             # Data access
├── storage/                    # Persistence
│   ├── db/                     # Room database
│   ├── ExtensionStorage.kt    # KV storage per extension
│   ├── ExtensionFileManager.kt
│   └── AppPreferences.kt      # Encrypted app settings
└── di/                         # Hilt modules
```

---

## Security Model

| Aspect | Implementation |
|---|---|
| **Execution Scope** | Extensions run only on URL-matched pages |
| **Permission Gating** | APIs require declared permissions |
| **Storage Isolation** | Each extension has namespaced storage |
| **Bridge Hardening** | `Object.freeze()` prevents API tampering |
| **Scope Isolation** | Scripts wrapped in IIFEs |
| **File Access** | WebView file:// access disabled |
| **Path Sanitization** | Extension IDs sanitized to prevent traversal |
| **API Key Storage** | EncryptedSharedPreferences |
| **AI Rate Limiting** | Gemini free tier limits (15 RPM) |

> **Honest Limitation:** Extensions share the WebView JS context. A malicious extension can access page DOM and other extensions' global state. This is the same limitation as Tampermonkey/Greasemonkey. True isolation requires separate processes, which is out of scope for the MVP.

---

## Sample Extensions

### Dark Mode (`sample-extensions/dark-mode/`)
Applies a dark theme to any webpage. Demonstrates CSS injection and storage API.

### Word Counter (`sample-extensions/word-counter/`)
Shows a floating badge with the page word count. Demonstrates DOM manipulation with zero permissions.

### AI Page Assistant (`sample-extensions/ai-assistant/`)
Adds a floating ✨ button with AI-powered summarize and ask features. Demonstrates `mer.ai.*` and `mer.overlay.*` APIs.

---

## Roadmap

- [x] Browser with tabs and URL bar
- [x] Extension manifest system
- [x] JS/CSS injection engine
- [x] Runtime bridge (Kotlin ↔ JS)
- [x] Storage API
- [x] URL pattern matching
- [x] AI integration (Gemini)
- [x] Overlay API
- [ ] Multi-tab WebView management (currently single WebView, multiple tab states)
- [ ] Extension update mechanism
- [ ] `document_start` injection support
- [ ] Async bridge API (Promise-based storage)
- [ ] Extension marketplace / URL install
- [ ] CSP enforcement for extensions
- [ ] Background scripts via WorkManager
- [ ] GeckoView engine option
- [ ] History & bookmarks

---

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
