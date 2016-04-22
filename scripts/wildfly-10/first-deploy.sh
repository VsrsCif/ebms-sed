#!/bin/sh

SED_EBMS=$1
JBOSS_HOME=$2
SED_HOME=$3

# create module folder
mkdir -p  "$JBOSS_HOME/modules/org/sed/main/"
# copy module libraries
cp "$SED_EBMS/modules/ebms-msh-xsd-1.0.jar" "$JBOSS_HOME/modules/org/sed/main/"
cp "$SED_EBMS/modules/ebms-sed-wsdl-1.0.jar" "$JBOSS_HOME/modules/org/sed/main/"
cp "$SED_EBMS/modules/ebms-sed-commons-1.0.jar" "$JBOSS_HOME/modules/org/sed/main/"
# copy module descriptor
cp "$SED_EBMS/modules/org.sed.module.xml" "$JBOSS_HOME/modules/org/sed/main/module.xml"
# set fix for module org.apache.ws.security
cp "$SED_EBMS/modules/org.apache.ws.securitymodule.xml" "$JBOSS_HOME/modules/system/layers/base/org/apache/ws/security/main/module.xml"


# deploy commons ejbs
cp "$SED_EBMS/deployments/ebms-sed-dao.jar"  "$JBOSS_HOME/standalone/deployments/"
cp "$SED_EBMS/deployments/sed-basic-tasks.jar"  "$JBOSS_HOME/standalone/deployments/"
# deploy modules 
cp "$SED_EBMS/deployments/ebms-msh.ear"  "$JBOSS_HOME/standalone/deployments/"
cp "$SED_EBMS/deployments/ebms-sed-ws.war"  "$JBOSS_HOME/standalone/deployments/"
cp "$SED_EBMS/deployments/ebms-sed-webgui.war"  "$JBOSS_HOME/standalone/deployments/"
cp "$SED_EBMS/deployments/plugin-zpp.war"  "$JBOSS_HOME/standalone/deployments/"
# copy configuration
cp "$SED_EBMS/widlfly-10/config/sed-roles.properties" "$JBOSS_HOME/standalone/configuration/"
cp "$SED_EBMS/widlfly-10/config/sed-users.properties" "$JBOSS_HOME/standalone/configuration/"
cp "$SED_EBMS/widlfly-10/config/standalone-ebms.xml" "$JBOSS_HOME/standalone/configuration/"

# copy start scripts
cp "$SED_EBMS/widlfly-10/sed-first-start.sh" "$JBOSS_HOME/bin/"
chmod u+x "$JBOSS_HOME/bin/sed-first-start.sh"

# create home folder
cp -r "$SED_EBMS/sed-home" "$SED_HOME"






# deploy module
# deploy settings + start script
# deploy application
# first start of widlfly
