# IoT Microservices Application

This project demonstrates a **Subscriber–Provider architecture** implemented using **two microservices** built with **Vert.x**.

## Architecture Overview

The system consists of two independent services:

- **Provider Service** – exposes APIs and publishes device-related data.
- **Subscriber Service** – subscribes to the provider APIs and consumes the data.

This model simulates a **provider–subscriber communication pattern** commonly used in IoT and event-driven systems.

## Technology Stack

- Java
- Vert.x
- PostgreSQL
- Docker
- REST APIs

## Microservice Communication

The two microservices communicate with each other using the **Vert.x Web Client**.

Specifically:

`io.vertx.ext.web.client.WebClient`

This client enables the **Subscriber service** to call the **Provider service APIs** asynchronously in a non-blocking manner.

## Database

The application uses **PostgreSQL** for data persistence.  
Database initialization scripts are included in the project.

## Deployment

Refer Deployment Document at the root

## Testing APIs

Once the containers are running, APIs can be tested using the provided HTML pages:

- `provider/provider-api-tester.html`
- `subscriber/subscriber-cors-test.html`

Simply open these files in a web browser and test the endpoints.

Alternatively, you can also use the **Postman collections** available inside each service directory to test the APIs.
