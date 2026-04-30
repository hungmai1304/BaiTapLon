# Online Auction System (BTL)

A Java-based desktop application for managing online auctions, developed for the "Advanced Programming" (Lập trình nâng cao) course. This project employs a Client-Server architecture with a JavaFX frontend and socket-based communication.

## 🚀 Key Features

- **User Roles:** Supports Bidder, Seller, and Admin roles with distinct permissions.
- **Item Categories:** Manage diverse auction items, including Electronics, Art, and Vehicles.
- **Advanced Bidding Engine:**
  - Concurrent bidding handling.
  - Auto-bidding system with user-defined configurations.
  - Anti-sniping logic to ensure fair competition.
- **Real-time Updates:** Utilizes the Observer pattern for instantaneous price updates and notifications.
- **Data Visualization:** Real-time bid history price curves for visual tracking of auction trends.

## 🏗️ Architecture

The system follows a distributed architecture:
- **Client:** JavaFX-based MVC (Controller + FXML) for a rich user experience.
- **Server:** MVC architecture (Controller -> Service -> DAO/Database) for robust business logic.
- **Communication:** Sockets with JSON data exchange using the Jackson library.

## 🛠️ Technologies Used

- **Java 17:** Core programming language.
- **JavaFX:** UI framework for the desktop client.
- **Maven:** Dependency management and build automation.
- **Jackson:** JSON processing for network communication.
- **JUnit 5:** Unit testing framework.

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/com/auction/
│   │   ├── client/     # JavaFX controllers and networking client
│   │   ├── common/     # Shared models (User, Item, Auction), exceptions, and protocols
│   │   ├── server/     # Server-side services, DAOs, and controllers
│   │   └── protocol/   # JSON request/response definitions
│   └── resources/      # FXML view files and static assets
└── test/               # Unit tests for core services and business logic
```

## ⚙️ Getting Started

### Prerequisites
- **JDK 17** or higher
- **Maven 3.x**

### Installation
1. Clone the repository.
2. Build the project:
   ```bash
   mvn clean install
   ```

### Running the Application

1. **Start the Server:**
   ```bash
   java -cp target/classes com.auction.server.service.AuctionServer
   ```
2. **Start the Client:**
   ```bash
   mvn javafx:run
   ```

## 🧪 Testing
Run the automated test suite using Maven:
```bash
mvn test
```

## 📐 Design Patterns & Standards
- **Singleton:** `AuctionManager` for centralized auction state.
- **Factory Method:** `ItemFactory` for extensible item creation.
- **Observer:** Real-time event propagation to clients.
- **Coding Style:** Follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
