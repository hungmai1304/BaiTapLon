# Gemini Context - Online Auction System (BTL)

This project is a Java-based Online Auction System developed for the "Advanced Programming" (Lập trình nâng cao) course. It follows a Client-Server architecture with a JavaFX frontend.

## Project Overview
- **Type:** Java Desktop Application (Client-Server)
- **Architecture:**
  - **Client:** JavaFX-based MVC (Controller + FXML)
  - **Server:** MVC (Controller -> Service -> DAO/Database)
  - **Communication:** Sockets with JSON data exchange (Jackson)
- **Core Technologies:** Java 17, JavaFX, Maven, Jackson (JSON), JUnit 5
- **Key Features:**
  - User Roles: Bidder, Seller, Admin
  - Item Categories: Electronics, Art, Vehicle
  - Bidding Engine: Concurrent bidding handling, Auto-bidding, Anti-sniping logic
  - Real-time Updates: Observer pattern for price updates and notifications
  - Visualization: Real-time bid history price curves

## Technical Specifications
- **Design Patterns:**
  - **Singleton:** `AuctionManager`
  - **Factory Method:** `ItemFactory` for creating item types
  - **Observer:** Real-time auction event management
  - **Strategy/Command:** Specialized bidding logic
- **Development Standards:**
  - **Coding Style:** Google Java Style Guide
  - **Testing:** Unit testing with JUnit 5
  - **Source Control:** Git with conventional commits

## Building and Running

### Prerequisites
- JDK 17
- Maven 3.x

### Build Commands
```bash
# Clean and compile the project
mvn clean install

# Run tests
mvn test
```

### Execution Commands
- **Start Server:**
  ```bash
  # TODO: Verify server main class, likely:
  java -cp target/classes com.auction.server.service.AuctionServer
  ```
- **Start Client:**
  ```bash
  mvn javafx:run
  ```

## Project Structure
- `src/main/java/com/auction/client`: Client-side logic and JavaFX controllers.
- `src/main/java/com/auction/server`: Server-side services, DAOs, and controllers.
- `src/main/java/com/auction/common`: Shared models, exceptions, and protocols.
- `src/main/resources`: FXML views and static assets.
- `src/test`: Unit tests for core services.

## Development Status
The project currently contains the skeleton structure and entity definitions. Core logic (AuctionManager, ClientHandler) and UI implementations (FXML) are in the early stages of development.
