# Hearing Recording Storage Service

#.github custom workflows

https://github.com/hrvey/combine-prs-workflow
 is used to combine passing dependabot PRs into a single branch

## Pre-requisites:

To be able to run the applicaiton locally, you will need to be able to run the docker images
for CCD and other services.

You will need to be able to run this command:
az acr login --name hmctspublic && az acr login --name hmctsprivate

So standard az cli tools are needed, as well as @hmcts.net log in with appropriate roles

## Setup

Simply run the following script to start all application dependencies.

```bash
  ./docker/dependencies/start-local-environment.sh
```
### Building the application

To build the project execute the following command:

```bash
  ./gradlew build
```
### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the application in docker by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port [8080]

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8080/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

#Gotchas:

1) the applicaitons postgres database runs on port 5444 due to issues getting pact broker database
to run on a different port

2) as with all liquibase projects, sometimes your database will be out of sync with changes within
   src/main/resources/db/db.changelog-master.xml
   AS a convenience method, when your run "make app-run", it will call the liquibase apply as part of the
   gradle commands. this make command resolves to:
   ./gradlew migratePostgresDatabase bootRun

3) before running any integration tests from hrs-ingestor, you will need to prime the CCD data API with the
   hrs spreadsheet. This can be achieved by running ./gradlew functional

4) Sonarqube only does analysis

#Local Dev

##First Time Build

You'll need to get sonarqube, and initialise it and change the password to adminnew

to fetch the latest image, run it and open the browser
run:
make sonarqube-fetch-sonarqube-latest
make report-sonarqube

in the browser, log in as admin (password=admin), go to http://localhost:9000/account/security/ and change password to adminnew


##Subsequent Builds (these must all pass before raising a PR)

checks:
 - make check-all

sonarqube:
 - make sonarqube-run-local-sonarqube-server
 - sonarqube-run-tests-with-password-as-adminnew

smoketest:

 - make docker-compose-dependencies-up
 - make app-run
 - make app-smoke-test

#Connecting to Database
Using PGAdmin, or IntelliJ Ultimate:

host:localhost
port:5444
username:emhrs
pass:emhrs
jdbc_url: jdbc:postgresql://localhost:5444/emhrs


#Idea Setup

Increase import star to 200 to avoid conflicts with checkstyle
https://intellij-support.jetbrains.com/hc/en-us/community/posts/206203659-Turn-off-Wildcard-imports-

Auto import of non ambiguous imports
https://mkyong.com/intellij/eclipse-ctrl-shift-o-in-intellij-idea/#:~:text=In%20Eclipse%2C%20you%20press%20CTRL,imports%2C%20never%20imports%20any%20package.

Import the checkstyle code scheme into the java code settings

Reverse the import layout settings / modify until the checkstyle passes
Uncheck "Comment at first column"

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

Refresh staging pod
