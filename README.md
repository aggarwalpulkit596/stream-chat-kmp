# Stream Chat KMP SDK

A modern, type-safe, and platform-agnostic implementation of the Stream Chat SDK using Kotlin Multiplatform (KMP). This project demonstrates how to build a production-ready chat SDK that works seamlessly across Android, iOS, JavaScript (Browser/Node.js), and Desktop platforms while maintaining native performance and user experience.

## 🚀 Features

- **True Multiplatform Support**
  - Android (Kotlin/JVM)
  - iOS (Kotlin/Native)
  - JavaScript (Browser & Node.js)
  - Desktop (Kotlin/JVM)
  - Shared business logic and data models
  - Platform-specific optimizations

- **Modern Architecture**
  - Clean Architecture principles
  - Dependency Injection ready
  - Coroutines for asynchronous operations
  - Kotlin Flow for reactive streams
  - Kotlin Serialization for type-safe JSON handling
  - Ktor for HTTP networking

- **Robust Authentication & Security**
  - JWT token management
  - Secure token storage (EncryptedSharedPreferences, Keychain, etc.)
  - Platform-specific SSL/TLS configurations
  - Automatic token refresh
  - Anonymous user support

- **Comprehensive API Client**
  - Type-safe API endpoints
  - Automatic request/response serialization
  - Retry policies with exponential backoff
  - Rate limiting awareness
  - Connection pooling
  - Comprehensive error handling

- **Platform-Specific Optimizations**
  - Android: OkHttp engine with connection pooling
  - iOS: Darwin engine with native networking
  - JavaScript: Browser/Node.js specific engines
  - Desktop: CIO engine with custom configurations

## 🏗 Architecture

The SDK follows clean architecture principles with clear separation of concerns:

```
shared/
├── commonMain/
│   ├── auth/           # Authentication & token management
│   ├── client/         # HTTP client & API implementation
│   ├── models/         # Shared data models
│   └── utils/          # Common utilities
├── androidMain/        # Android-specific implementations
├── iosMain/           # iOS-specific implementations
├── jsMain/            # JavaScript-specific implementations
└── desktopMain/       # Desktop-specific implementations
```

### Key Components

1. **ChatClientConfig**
   - Environment-specific configurations
   - Platform-agnostic settings
   - Builder pattern for easy configuration
   - Type-safe configuration options

2. **ChatHttpClient**
   - Platform-specific HTTP engines
   - Connection pooling
   - SSL/TLS handling
   - Request/response interceptors

3. **ChatApiClient**
   - REST API implementation
   - Rate limiting
   - Error handling
   - Response parsing

4. **TokenManager**
   - JWT token management
   - Secure storage
   - Automatic refresh
   - Session management

## 🛠 Technical Highlights

### 1. Kotlin Multiplatform Features
- Shared business logic across platforms
- Platform-specific implementations where needed
- Type-safe serialization
- Coroutines for async operations
- Flow for reactive streams

### 2. Security
- Platform-specific secure storage
  - Android: EncryptedSharedPreferences
  - iOS: Keychain
  - JavaScript: Encrypted localStorage/Node.js secure storage
  - Desktop: Encrypted file storage
- SSL/TLS configurations
- JWT token management
- Secure connection handling

### 3. Networking
- Ktor HTTP client
- Platform-specific engines
- Connection pooling
- Retry policies
- Rate limiting
- Request/response logging

### 4. Error Handling
- Comprehensive error types
- Platform-specific error mapping
- Retry strategies
- Rate limit handling
- Network error recovery

## 📱 Platform Support

### Android
```kotlin
val config = ChatClientConfig.builder()
    .apiKey("your-api-key")
    .baseUrl("https://chat.stream-io-api.com")
    .platformConfig {
        context(applicationContext)
        isDebug(BuildConfig.DEBUG)
    }
    .build()

val client = ChatClient(config)
```

### iOS
```kotlin
let config = ChatClientConfig.builder()
    .apiKey("your-api-key")
    .baseUrl("https://chat.stream-io-api.com")
    .platformConfig {
        isDebug(DEBUG)
    }
    .build()

let client = ChatClient(config)
```

### JavaScript
```kotlin
const config = ChatClientConfig.builder()
    .apiKey("your-api-key")
    .baseUrl("https://chat.stream-io-api.com")
    .platformConfig {
        version("1.0.0")
        isDebug(process.env.NODE_ENV === "development")
    }
    .build()

const client = new ChatClient(config)
```

### Desktop
```kotlin
val config = ChatClientConfig.builder()
    .apiKey("your-api-key")
    .baseUrl("https://chat.stream-io-api.com")
    .platformConfig {
        version("1.0.0")
        isDebug(System.getProperty("app.debug") == "true")
    }
    .build()

val client = ChatClient(config)
```

## 🎯 Why Kotlin Multiplatform?

1. **Code Reuse**
   - Share business logic across platforms
   - Reduce code duplication
   - Maintain consistency
   - Faster development cycles

2. **Type Safety**
   - Kotlin's strong type system
   - Null safety
   - Platform-specific type mapping
   - Compile-time error detection

3. **Performance**
   - Native performance on each platform
   - Platform-specific optimizations
   - Efficient memory management
   - Minimal runtime overhead

4. **Developer Experience**
   - Single language (Kotlin)
   - Familiar tooling
   - Great IDE support
   - Easy debugging

## 🛠 Development Setup

1. **Prerequisites**
   - Kotlin 1.9.0+
   - Android Studio Arctic Fox+
   - Xcode 13+ (for iOS)
   - Node.js 14+ (for JavaScript)
   - JDK 11+ (for Desktop)

2. **Build**
   ```bash
   ./gradlew build
   ```

3. **Run Tests**
   ```bash
   ./gradlew test
   ```

4. **Generate Documentation**
   ```bash
   ./gradlew dokkaHtml
   ```

## 📚 Documentation

- [API Reference](docs/api.md)
- [Platform Setup](docs/platform-setup.md)
- [Authentication Guide](docs/auth.md)
- [Best Practices](docs/best-practices.md)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Stream Chat](https://getstream.io/chat/) for the amazing chat API
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) team
- [Ktor](https://ktor.io/) for the networking library
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) for JSON handling

## 👨‍💻 Author

Pulkit Aggarwal - https://github.com/aggarwalpulkit596/

## 🔮 Future Plans

- [ ] WebSocket support for real-time updates
- [ ] Offline message queue
- [ ] Message encryption
- [ ] File upload/download progress
- [ ] Push notification support
- [ ] Analytics integration
- [ ] More platform-specific optimizations

---

This project demonstrates advanced Kotlin Multiplatform development skills, including:
- Cross-platform architecture design
- Platform-specific optimizations
- Security best practices
- Modern Kotlin features
- Clean code principles
- Type-safe APIs
- Comprehensive error handling
- Performance considerations 
