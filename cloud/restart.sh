#!/bin/bash

# Environment variables DEPLOY_DIR and LOG_DIR are always set by the calling hook script.
# Also all the "run_environment" variables in the ansible group_vars are exported by the hook script.

export JAVA_OPTS="\
-Dtor.profile=cloud \
-Dtor.port=8080 \
-Dlog4j.configuration=file://$DEPLOY_DIR/src/main/resources/log4j.cloud.properties \
-Dlog4j.log.dir=$LOG_DIR \
-Dldap.host=ldap \
-Dldap.password=$LDAP_PASSWORD \
-Dldap.userdn=$LDAP_USERDN \
-Dopintopolku.virkailija.url=\"https://testi.virkailija.opintopolku.fi\" \
-Dauthentication-service.username=$AUTHENTICATION_SERVICE_USERNAME \
-Dauthentication-service.password=$AUTHENTICATION_SERVICE_PASSWORD \
"

pkill java
make build && { nohup make run &>> $LOG_DIR/tor.stdout.log & }
