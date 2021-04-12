docker-compose:
	docker-compose up

docker-compose-dependencies-up:
	docker-compose -f docker-compose-dependencies.yml up

docker-compose-dependencies-down:
	docker-compose -f docker-compose-dependencies.yml down

liquibase-create-change-log:
	./gradlew liquibaseDiffChangelog

liquibase-apply-change-log:
	./gradlew migratePostgresDatabase

#applies database changes if required, and runs spring bootapp
app-run:
	./gradlew migratePostgresDatabase bootRun

app-clean-run:
	./gradlew clean bootRun

app-smoke-test:
	./gradlew smoke -i

test-functional:
	./gradlew functional -i

test-integration:
	./gradlew integration -i

test-code:
	./gradlew test -i

check-code:
	./gradlew check -i

check-dependencies:
	./gradlew dependencyCheckAggregate -i

check-coverage:
	./gradlew test integration  jacocoTestCoverageVerification jacocoTestReport && open build/reports/jacoco/test/html/index.html

check-all:
	./gradlew test integration check dependencyCheckAggregate jacocoTestCoverageVerification jacocoTestReport && open	build/reports/jacoco/test/html/index.html

#Note this fails if there is already a container.
sonarqube-run-local-sonarqube-server:
	docker start sonarqube

sonarqube-fetch-sonarqube-latest:
	docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest

# New containers will require logging in, and changing password to a temporary password, and back to admin
sonarqube-run-tests-with-password-as-admin:
	./gradlew sonarqube -Dsonar.login="admin" -Dsonar.password="admin" -i

sonarqube-run-tests-with-password-as-adminnew:
	./gradlew sonarqube -Dsonar.login="admin" -Dsonar.password="adminnew" -i && open http://localhost:9000/

report-sonarqube:
	xdg-open http://localhost:9000/

report-checkstyle:
	xdg-open build/reports/checkstyle/main.html

report-code-tests:
	xdg-open build/reports/tests/test/index.html

report-integration-tests:
	xdg-open build/reports/tests/integration/index.html

report-smoke-tests:
	xdg-open build/reports/tests/smoke/index.html

report-code-pmd-main:
	xdg-open build/reports/pmd/main.html

report-code-pmd-test:
	xdg-open build/reports/pmd/test.html

report-code-pmd-integration-test:
	xdg-open build/reports/pmd/integrationTest.html

report-code-pmd-smoke-test:
	xdg-open build/reports/pmd/smokeTest.html

report-dependency-check:
	xdg-open build/reports/dependency-check-report.html

report-jacoco:
	xdg-open build/reports/jacoco/test/html/index.html
