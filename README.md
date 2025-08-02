[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)
[![codecov](https://codecov.io/github/wichayutdew/paidy-assignment/graph/badge.svg?token=JF4LSGB9B6)](https://codecov.io/github/wichayutdew/paidy-assignment)

# Dew's version of the `README.md` file

> This is a modified version of the original [README.md](OLD_README.md) file.

# Notes

The codebase will follow [conventional commit](https://www.conventionalcommits.org/en/v1.0.0/). So, both the commit
message and the PR title will follow Semantic convention.

- TL;DR, follow this example `feat: add new feature` for commit message and PR title. and change the prefix based on the
  type of change you made

> - feat: (new feature for the user, not a new feature for build script)
> - fix: (bug fix for the user, not a fix to a build script)
> - docs: (changes to the documentation)
> - style: (formatting; no production code change)
> - refactor: (refactoring production code, eg. renaming a variable)
> - test: (adding missing tests, refactoring tests; no production code change)
> - chore: (updating grunt tasks e.g. update CI/CD, Docker runner, etc; no production code change)

# Overview

1. As the requirement is vague, I'll list my assumptions in each task to clarify the scope of the task.
2. Since I don't want to over-complicated the task, I will not try to deploy this code to any cloud provider but rather
   make sure local development and testing is as smooth as possible.
    - Means, all the external dependencies will be deployed locally under single docker-compose file. All the steps to
      start the services will be documented in the `README.md` file.

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
- since `scoverage` transitive dependency is crashing with `scalafmt-coursier`, I decided to use normal `scalafmt`
  instead

## [Handle Errors](https://github.com/wichayutdew/paidy-assignment/pull/5)

> As checked in the original code, the service does not handle errors properly. This is to ensure the API do throws
> appropriate/actionable errors to users when not working as expected

- Implement request parameters validation
    - *Assumption here is the Currency in [Currency.scala](forex-mtl/src/main/scala/forex/domain/Currency.scala) Class
      consists all the valid currencies and nothing more.
- Implement appropriate error handler returns from service level
    - As OneFrame API is still Dummy, the error is not being thrown in service yet.

## [Initiate test framework with Scalatest](https://github.com/wichayutdew/paidy-assignment/pull/5)

> Set up the test framework to ensure the code is covered by tests and add test case to covers existing code

- the test framework is set up with `scalatest` and `mockito-scalatest`

## [Connect to One Frame API](https://github.com/wichayutdew/paidy-assignment/pull/8)

> Implement the live interpreter to connect to the One Frame API to satisfy the functional requirements of getting the
> exchange rate

### Assumptions

1. The requirement doesn't specifically mention the need to hide Forex API endpoint under authentication logic
   > So, I assume the responsibility of Forex service as an information conveyer to Paidy's internal service and leave
   the endpoint open.
   > Since the backend service will usually be hidden behind company's network. and public network will not be able to
   hit it unless we expose it.
   > But if this is a real world I wouldn't assume and probably consult with the team/Product on the nature of the
   service and how would it being utilized.
2. One Frame API response returns more fields than the service expects
   > For me, personally, if the requirement is vague, I would assume that the service signature should remain the same.
   > If there's an extra requirement to expose more fields, it can be done in the future once the requirement is clear.
3. HTTP client to use will be `http4s`
   > Since we develop the http server in this service with http4s, by using client from same library, the code will be
   more consistent and easier to maintain.
   > This prevents any other transitive dependency issues that may arise from using different HTTP client library.
4. OneFrame Token will not be stored in the codebase
   > To keep the codebase clean and secure, I will not store the OneFrame Token in the codebase. within the current
   changes and will be handled in the next task.

## [Move secret to Vault](https://github.com/wichayutdew/paidy-assignment/pull/9)

> Proper way to deals with secrets and keep codebase secure is to saved it to secret manager.
>
> But as we're not deploying this to any cloud provider, we don't get to use any services provided by cloud provider.
>
> I will use local [HashiCorp Vault](https://www.vaultproject.io/) to store all the secrets instead.
>
> And as Scala doesn't have stable native library to connect to Vault, I'll utilize one of the JVM languages perks "
> using Java library".

### Assumptions

1. As vault usually holds long-lived token, it's a good idea to cache the token in memory
   > I don't consider caching it in Redis since it's too risky to pass token through another network hop risking it to
   be compromised
   > As the nature of cloud deployment we do containerized the service and split it into multiple pods, this means each
   pod will need to fetch the token from vault separately, without a cache, that's mean we multiply external calls by 2
   every time we hit OneframeAPI.
   > And also nature of vault service, it's not designed to bear heavy loads, so caching the token in memory help
   relieve the load and still keeps the token secure.
   > Another point secret rotation is not done that frequently and once the secret rotates, we usually have a grace
   period where the old token is still valid while we are migrating to new one.
   > We can set up a cache ttl to be in hours fashion and reduce the load even further, and with help of CI/CD, we will
   also do deployment quite often, this will also help refresh the memory cache as well.

## [Build Redis External Cache](https://github.com/wichayutdew/paidy-assignment/pull/10)

> Implement the Redis external cache to extends to satisfy non-functional requirements of 10,000 successful requests
> with not more than 5 minutes rate per day

### Assumptions

1. One Frame API allows to fetch multiple exchange rates in a single request, So, I will boot a service by filling Redis
   cache with all possible pairs since it would only consume 1 request to One Frame API
   > As from my observation from the API response, rates for pair opposite pair is not the same as the original pair
   > For example, if I request `USD` to `JPY`, the rate is not the same as `JPY` to `USD`. So total from allowed 9
   Currencies there will be 36*2 = 72 possible pairs.
   > With this cache preparation, for first 5 minutes, after the service is booted, we will be able to save multiple
   requests to One Frame API.
2. Initially thought of refreshing entire cache on every api call but decide not to implement.
   > At first, I thought of refreshing all the rates cacehe on every API call.
   > But then I realized that on a production environment, the respone time on One Frame API might be worse if we fetch
   all the 72 pairs at once.
   > That's why I decide to not implement it and just fetch only the requested pair to keep latency low.
   >
   > But thing will change if we are able to get to know logic behind One Frame API and turns our fetching all the rates
   at once wouldn't increase the latency by a lot.
   > In that case, this will benefit the Forex service's latency by a lot.

### Caveat

1. Assume that the requirements of 10,000 successful requests per day will not span too long in a period, otherwise this
   requirement will not be possible to satisfy.
   > e.g. if the requirements are span over 24 hours with very low traffic, the 5 minutes cache will be expired before
   next call invoked, this result in another OneFrame API call.
   > and let's say this happen for over 1,000 times, we will eventually hit their rate limit.
   >
   > In this case, if it's an actual production service, I would either suggest to connect with OneFrame and ask them to
   raise their rate limit, e.g. into 100 requests per minute, etc. th prevent this issue.
   > Or we consult with product team whether how up to date the rate should be, if we can adjust it to be longer, the
   rate limiting issue should not be that severe.
   >
   > But anyway, before we reach that point, we need to at least have some supporting data to show that it's not
   physically possible to satisfy the requirement. That's leads to the next 2 task that I'll implement. Prometheus

## Build Integration Tests

> To ensure the service connects to the One Frame API and Redis external cache correctly

### Assumptions

1. We rely a lot on circe encoder/decoder to do serialization of OneFrame API response and Redis caching.
   > This might ends up in failure loop in case OneFrame API decides to change their response format or Redis somehow
   saving cache in wrong format.
   > Unit/Integration/E2E test will at least confirm that the 2nd scenario where redis is not saving cache correctly
   will not be happening.
   > But anyway we cannot really do fool-proofing against the first scenario, so we will just have to monitor the
   service and fix it when it happens.

## Send a server metric to Prometheus

> To have a monitoring system to monitor the service's non-functional requirements

## [Extras] Load test

> To ensure the service can handle 10,000 successful requests per day

## [Optional] Code Refactoring/Cleanup

> In case there is any code that can be improved or cleaned up
1. convert returned timestamp to be in server timezone
   > this will be helpful for internal service within company in same DC to avoid timezone conversion issues

## Idea

1. make generic HTTP client and Server Route
2. convert Error to GenericServerError so we don't need to handle error transformation in every service -- this goes
   against the functional programming paradigm, so I'll leave it as an optional task
3. Hide sensitive error message from user
4. create DOCKERFILE to allow service to run in production, split application config into `common` - qa,prod,local,test
5. create cache invalidate endpoint behind authentication to allow internal user to invalidate the cache easily. In
   case, we need to promptly update the token from vault, or there's some wrong exchange data 
