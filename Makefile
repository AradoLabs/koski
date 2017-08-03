env = cloud
cleandist = true
mvn_argline =
mvn_opts =

help:
	@echo ""
	@echo "make build	- Build the whole application, ready for running or testing"
	@echo "make front	- Build front end"
	@echo "make codegen	- Generate database access code from local Postgres database"
	@echo "make test	- Run unit tests"
	@echo "make run	- Run previously built application in local environment"
	@echo "make postgres	- Run local postgres server"
	@echo "make watch	- Watch for changes in webapp files"
	@echo "make deploy 	- Deploy to CSC's ePouta cloud"
	@echo "make dist version=<version> - Tag and deploy application to artifactory."
	@echo "make deploy env=<env> version=<version>	- Install deployed version to env."

logdir:
	@mkdir -p log
clean:
	mvn clean
	rm -fr web/target

### Building the application

build: front
	mvn compile
	# Built the whole application, ready for running or testing
front: logdir
	cd web && mkdir -p target && npm install
watch:
	cd web && npm run watch
source-to-image: build
	echo "TODO" > target/webapp/buildversion.txt
	mvn package -DskipTests

### Running tests

test: front
	mvn $(mvn_opts) -DargLine="$(mvn_argline)" test
testresults:
	less +`grep -n "FAILED" target/surefire-reports/koski-tests.txt|head -1|cut -d ':' -f 1` target/surefire-reports/koski-tests.txt
js-unit-test:
	cd web && npm run unit-test
js-unit-test-watch:
	cd web && npm run unit-test-watch
fronttest:
	cd web && npm run test
screenshot:
	ls -t web/target/screenshots|head -1|xargs -I{} open web/target/screenshots/{}

### Running application and database 

run:
	mvn exec:java $(JAVA_OPTS) -Dexec.mainClass=fi.oph.koski.jettylauncher.JettyLauncher
postgres:
	postgres --config_file=postgresql/postgresql.conf -D postgresql/data
postgres-clean:
	rm postgresql/data/postmaster.pid 2> /dev/null||true
elastic:
	elasticsearch -E path.conf=elasticsearch -E path.data=elasticsearch/data -E path.logs=elasticsearch/log

### Code checks

eslint: front
	cd web && npm run lint
scalastyle:
	mvn verify -DskipTests -P scalastyle
lint: eslint scalastyle
owasp: 
	mvn dependency-check:check -P owasp
owaspresults:
	open target/dependency-check-report.html
snyk: # javascript dependency vulnerability check
	cd web && npm install && node_modules/snyk/cli/index.js test
mvndeps:
	mvn dependency:tree|less
scala-console:
	./scripts/mvn-scala-console.sh

### Dist and deploy

dist: check-version
	cleandist=$(cleandist) ./scripts/dist.sh $(version)
deploy: check-version
	./scripts/deploy.sh $(env) $(version)
check-version:
ifndef version
		@echo "version is not set."
		@echo "Set version with version=<version>"
		@echo "Use version=local for locally installed version"
		exit 1
endif
great:
again:
