# Forex-MTL

## Pre-requisites
- Make sure you have the following installed on your machine either globally or in IDE:
  - __SBT 1.8.0__
  - __Java 17__ - _Preferably Azul Zulu, as I experienced the least side effects with it. compared to OpenJDK, Amazon Corretto, etc._
  - Docker installed and running - _to run all external dependencies e.g. One Frame API, Redis, etc._

## Running the project locally
```bash
cd forex-mtl
docker compose -f dependencies.yml up -d
sbt run
```

## useful SBT commands
```bash
sbt clean ## Clean up all the generated files to prevent unexpected behavior

sbt compile ## Compile the project

sbt run ## Starts the application

sbt scalafmtAll ## format the codebase per .scalafmt.conf

sbt scalafmtCheckAll ## check the codebase format per .scalafmt.conf

sbt test ## Run all tests

sbt coverage test ## Run all tests with code coverage

sbt coverageReport ## Generate the code coverage report
```
