[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)

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

# Project Setup 
First step to make sure the project runs smoothly/with quality standard set. And extensible to all other developers.

## Development Environment
- Create local development and guide so that other developers can run the project locally
> I will skip the deployment since we would not be able to deploy the project without cloud access

## CI/CD
- create Github Action's workflow to maintain code quality including:
  - sbt clean compile -- To compile and do format check
  - sbt test -- To run all tests
  - [Optional] Add code coverage report with CodeCov intregration
  - [Optional] Add integration test to test connection with other services e.g. database, external API, etc.
