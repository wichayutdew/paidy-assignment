[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)
[![codecov](https://codecov.io/github/wichayutdew/paidy-assignment/graph/badge.svg?token=JF4LSGB9B6)](https://codecov.io/github/wichayutdew/paidy-assignment)

# Dew's version of the `README.md` file
> This is a modified version of the original [README.md](OLD_README.md) file.

# Notes
The codebase will follow [conventional commit](https://www.conventionalcommits.org/en/v1.0.0/). So, both the commit message and the PR title will follow Semantic convention.
  - TL;DR, follow this example `feat: add new feature` for commit message and PR title. and change the prefix based on the type of change you made
  >   - feat: (new feature for the user, not a new feature for build script)
  >   - fix: (bug fix for the user, not a fix to a build script)
  >   - docs: (changes to the documentation)
  >   - style: (formatting; no production code change)
  >   - refactor: (refactoring production code, eg. renaming a variable)
  >   - test: (adding missing tests, refactoring tests; no production code change)
  >   - chore: (updating grunt tasks e.g. update CI/CD, Docker runner, etc; no production code change)

# Tasks Breakdown
## [Project Setup](https://github.com/wichayutdew/paidy-assignment/pull/1)
> Trying to understand the project and set up the local environment
- build fresh README.md
- Setup developer environment on both Terminal and IDE
- Familiarize with the sbt commands
## [CI/CD](https://github.com/wichayutdew/paidy-assignment/pull/2)
> Set up the CI/CD pipeline to ensure code quality and have a structured pull request template
- Create GitHun Actions to include build and test
## [Code Quality](https://github.com/wichayutdew/paidy-assignment/pull/3)
> Ensure code is covered by tests and follows the code quality standards
- Add code coverage tool `scoverage` and include it in the CI/CD pipeline with CodeCov
- since `scoverage` transitive dependency is crashing with `scalafmt-coursier`, I decided to use normal `scalafmt` instead
## [Handle Errors](https://github.com/wichayutdew/paidy-assignment/pull/4)
> As checked in the original code, the service does not handle errors properly. This is to ensure the API do throws appropriate/actionable errors to users when not working as expected
- Implement request parameters validation
  - *Assumption here is the Currency in [Currency.scala](forex-mtl/src/main/scala/forex/domain/Currency.scala) Class consists all the valid currencies and nothing more.
- Implement appropriate error handler returns from service level
  - As OneFrame API is still Dummy, the error is not being thrown in service yet.
## Initiate test framework with Scalatest
> Set up the test framework to ensure the code is covered by tests
## Connect to One Frame API
> Implement the live interpreter to connect to the One Frame API to satisfy the functional requirements of getting the exchange rate
## Move secret to Vault
> https://developer.hashicorp.com/vault/docs/get-started/developer-qs
## Build Redis External Cache
> Implement the Redis external cache to extends to satisfy non-functional requirements of 10,000 successful requests per day
## Build Integration Tests
> To ensure the service connects to the One Frame API and Redis external cache correctly
##  Send a server metric to Prometheus
> To have a monitoring system to monitor the service's non-functional requirements
## Build E2E Load Tests
> To ensure the service can handle 10,000 successful requests per day
## [Optional] Code Refactoring/Cleanup
> In case there is any code that can be improved or cleaned up
