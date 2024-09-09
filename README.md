# Hearing Recording Storage Service

## .github custom workflows

https://github.com/hrvey/combine-prs-workflow
 is used to combine passing dependabot PRs into a single branch

## Pre-requisites:

To be able to run the application locally, you will need to be able to run the docker images
for CCD and other services.


You will need to be able to run this command:

az login

So standard az cli tools are needed, as well as @hmcts.net log in with appropriate roles

In order for integration tests to run, a docker image is needed for the
postgres testcontainers.

For this to pull from hmcts ACR you must login to the ACR first:
```bash
az login # if not logged in already
az acr login --name hmctspublic
```

## Setup

#### To clone repo and prepare to pull containers:

```
git clone https://github.com/hmcts/em-hrs-api.git
cd em-hrs-api/
```

#### Clean and build the application:

Requires docker desktop running

```
./gradlew clean
./gradlew build
```

#### To run the application:

```
az login
./gradlew bootWithCCD
```

This will start the API container exposing the application's port [8080]

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8080/health
```

Contained within the response should be similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

#### Gotchas:

1) sometimes your database will be out of sync with changes
   To fix this run:
   ./gradlew migratePostgresDatabase

2) before running any integration tests from hrs-ingestor, you will need to prime the CCD data API with the
   hrs spreadsheet. This can be achieved by running ./gradlew functional

3) Sonarqube only does analysis

## Local Dev

### First Time Build

You'll need to get sonarqube, and initialise it and change the password to adminnew

to fetch the latest image, run it and open the browser
run:
make sonarqube-fetch-sonarqube-latest
make report-sonarqube

in the browser, log in as admin (password=admin), go to http://localhost:9000/account/security/ and change password to adminnew


### Subsequent Builds (these must all pass before raising a PR)

checks:
 - make check-all

sonarqube:
 - make sonarqube-run-local-sonarqube-server
 - sonarqube-run-tests-with-password-as-adminnew

smoketest:

 - make docker-compose-dependencies-up
 - make app-run
 - make app-smoke-test

## Connecting to Database
Using PGAdmin, or IntelliJ Ultimate:

host:localhost
port:6432
username:emhrs
pass:emhrs
jdbc_url: jdbc:postgresql://localhost:6432/emhrs


## Idea Setup

Increase import star to 200 to avoid conflicts with checkstyle
https://intellij-support.jetbrains.com/hc/en-us/community/posts/206203659-Turn-off-Wildcard-imports-

Auto import of non ambiguous imports
https://mkyong.com/intellij/eclipse-ctrl-shift-o-in-intellij-idea/#:~:text=In%20Eclipse%2C%20you%20press%20CTRL,imports%2C%20never%20imports%20any%20package.

Import the checkstyle code scheme into the java code settings

Reverse the import layout settings / modify until the checkstyle passes
Uncheck "Comment at first column"

## Swagger UI
To view our REST API go to http://{HOST}/swagger-ui/index.html
On local machine with server up and running, link to swagger is as below

>http://localhost:8080/swagger-ui/index.html
>if running on AAT, replace localhost with ingressHost data inside values.yaml class in the necessary component, making sure port number is also removed.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

Refresh staging pod
