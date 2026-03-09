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
