# Video Recommender API

A multi-module video recommendation system that combines a Java Spring Boot backend, MongoDB storage, and Spark-based videos recommendation.

## Project Overview

This repository contains two primary parts:

- **Backend**: Java + Spring Boot REST APIs for movie search/detail, rating, comments, user/admin operations, and media management.
- **Recommender**: Scala + Apache Spark MLlib jobs for collaborative filtering (ALS), plus auxiliary recommendation strategies.

At a high level:

1. User interactions (ratings/comments) are stored in MongoDB.
2. Offline Spark jobs generate recommendation results.
3. Backend APIs read recommendation collections and return results to clients.

## Tech Stack

- **Language**: Java, Scala
- **Backend**: Spring Boot, Spring Web, Lombok, Springdoc OpenAPI
- **Database**: MongoDB
- **Recommendation Engine**: Apache Spark, Spark MLlib ALS
- **Build**: Maven (multi-module)

## Repository Structure

```text
.
├── Backend/
│   ├── src/main/java/com/lgr/backend/
│   │   ├── controller/      # REST controllers
│   │   ├── service/         # Service interfaces and implementations
│   │   ├── repository/      # MongoDB data access layer
│   │   ├── model/           # Request/response/domain models
│   │   ├── config/          # Mongo/OpenAPI configuration
│   │   └── util/            # Result wrappers and utilities
│   └── src/main/resources/
│       ├── application.properties
│       └── scripts/         # MongoDB init/migration scripts
├── Recommender/
│   ├── DataLoader/          # Data loading pipeline
│   ├── CommonRecommender/   # Common/popularity strategies
│   └── CFRecommender/       # ALS collaborative filtering
└── pom.xml                  # Parent Maven config
```

## Core Features

### Backend APIs

- Movie fuzzy search and movie detail query
- Personalized recommendation endpoint (CF results)
- Most-viewed and top-rated recommendation endpoints
- Movie rating submission and score retrieval
- Comment management
- Admin-facing movie management and media upload

### Recommendation Pipeline

- Build rating matrix from MongoDB
- Train ALS model with Spark MLlib
- Generate top-N personalized recommendations per user
- Persist recommendation results back to MongoDB collections
