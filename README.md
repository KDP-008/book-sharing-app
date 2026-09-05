# Book Sharing App

A Spring Boot-based backend for a book-sharing platform where users can register, manage their book library, search, borrow or lend books. The project is designed to support a simple MVP and can be extended with more features such as recommendations, communities, and book clubs.

## What this project provides

This application includes the core functionality needed for a digital book-sharing marketplace:

- User registration, login, and password reset flows
- User profile and favorite genre/author preferences
- Book listing, search, and ownership management
- Borrowing lifecycle and delivery method handling
- Cart and checkout workflows for available books
- Notifications for due dates and return reminders

This project can be expanded into a production-ready system by adding features like:

- recommendations based on borrowing history
- favorites and wishlists
- messaging between book owners and borrowers
- request approvals and user moderation tools
- book clubs and community events
- advanced search and discovery
- integration with real databases, queues, and cloud services

I have included the user requirements document for the broader feature roadmap alongwith a system design for myself. Users are encouraged to create their own system design for their use case.

- [User Requirements](docs/01_book_sharing_app_requirements.md)
- [System Design](docs/02_book-sharing-app-design.drawio)


## Note to developers:

The application is currently configured to use an in-memory H2 database by default, which is ideal for local development and demos. This can be replaced with any relational or non-relational database according to production requirements.

## Project Technology stack

- Java 21
- Spring Boot 4.0.0
- Spring Web
- Spring Data JPA
- H2 in-memory database
- Maven
- JUnit 5
- Docker

## Local run steps

### Prerequisites

- Java 21+
- Maven 3.9+
- Git

### Run the app locally

```bash
cd /path/to/book-sharing-app
mvn clean install
mvn spring-boot:run
```

The application will start on:

- http://localhost:8080/api

The H2 database console is available at:

- http://localhost:8080/api/h2-console

Use the following default H2 connection settings:

- JDBC URL: jdbc:h2:mem:bookshare_db
- Username: sa
- Password: blank

### Run tests

```bash
cd /path/to/book-sharing-app
mvn test
```

## Docker setup

### Build the Docker image

```bash
cd /path/to/book-sharing-app
mvn clean package -DskipTests
docker build -t book-sharing-app .
```

### Run the container locally

```bash
docker run -p 8080:8080 --name book-sharing-app book-sharing-app
```

### Using Docker Compose

```bash
cd /path/to/book-sharing-app
docker compose up --build
```

This starts the app with the default in-memory H2 configuration.


## Extending the platform further

The project is designed to be extended beyond the MVP described in the requirements file. Additional capabilities that can be built next include:

- personalized recommendations and trending books
- author and genre follower logic
- wishlists and favorites
- messaging between borrowers and owners
- admin moderation for blocked users or suspended accounts
- community features like book clubs and events
- review and rating management
- advanced search filters and ranking
- analytics dashboards for lending activity
- notifications via email, SMS, or push channels

For more details, see the feature requirements in [docs/01_book_sharing_app_requirements.md](docs/01_book_sharing_app_requirements.md).
