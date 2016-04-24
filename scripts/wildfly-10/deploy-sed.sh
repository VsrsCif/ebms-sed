#!/bin/sh

# set fixed parameters
#WILDFLY_HOME=
#SED_BUNDLE=
#SED_HOME=
#INIT=


while [ "$#" -gt 0 ]
do
key="$1"

  case $key in
    -w|--wildfly)
      WILDFLY_HOME="$2"
      shift # past argument
    ;;
      -s|--sed-home)
      SED_HOME="$2"
      shift # past argument
    ;;
      -b|--bundle)
      SED_BUNDLE="$2"
      shift # past argument
    ;;
    --init)
      INIT="TRUE"
    ;;
    *)
      # unknown option
    ;;
  esac
  shift # past argument or value
done


if [ "x$WILDFLY_HOME" = "x" ]; then
	WILDFLY_HOME=$1
fi

if [ "x$SED_BUNDLE" = "x" ]; then
	SED_BUNDLE=$2
fi


if [ "x$SED_HOME" = "x" ]; then
	SED_HOME=$3
fi

quit () {
	echo "\nUsage:\n"
	echo "deploy-sed.bat --init -b [SED_BUNDLE] -w [WILDFLY_HOME] -s [SED_HOME]\n"
	echo "  --init  initialize sed-home and wildfly properties. "
	echo "  -w   WILDFLY_HOME -  path jboss home: ex.: c:\temp\wildfly-10.0.0.Final\."
	echo "  -b   SED_BUNDLE   - path to unziped ebms-sed bundle if not given parent script folder is setted."
	echo "  -s   SED_HOME     - path tom application home folder  (sed.home) if is not given and --init is setted than '[WILDFLY_HOME]\standalone\data\' is setted.	"
        exit
}

if [ "x$WILDFLY_HOME" = "x" ]; then
	quit;
fi

if [ ! -d "$WILDFLY_HOME" ]; then
	echo "WILDFLY_HOME folder not exists! Check parameters!"
	quit;
fi


if [ "x$SED_BUNDLE" = "x" ]; then
	DIRNAME=`dirname "$0"`
	SED_BUNDLE="$DIRNAME/../"
fi

if [ ! -d "$SED_BUNDLE" ]; then
	echo "SED_BUNDLE folder not exists! Check parameters!"
	quit;
fi



if [ "x$SED_HOME" = "x" ]; then
	SED_HOME="$WILDFLY_HOME/standalone/data/";
fi



# create module folder
mkdir -p  "$WILDFLY_HOME/modules/org/sed/main/"
# copy module libraries
cp "$SED_BUNDLE/modules/ebms-msh-xsd-1.0.jar" "$WILDFLY_HOME/modules/org/sed/main/"
cp "$SED_BUNDLE/modules/ebms-sed-wsdl-1.0.jar" "$WILDFLY_HOME/modules/org/sed/main/"
cp "$SED_BUNDLE/modules/ebms-sed-commons-1.0.jar" "$WILDFLY_HOME/modules/org/sed/main/"
# copy module descriptor
cp "$SED_BUNDLE/modules/org.sed.module.xml" "$WILDFLY_HOME/modules/org/sed/main/module.xml"



# deploy commons ejbs
cp "$SED_BUNDLE/deployments/ebms-sed-dao.jar"  "$WILDFLY_HOME/standalone/deployments/"
cp "$SED_BUNDLE/deployments/sed-basic-tasks.jar"  "$WILDFLY_HOME/standalone/deployments/"
# deploy modules 
cp "$SED_BUNDLE/deployments/ebms-msh.ear"  "$WILDFLY_HOME/standalone/deployments/"
cp "$SED_BUNDLE/deployments/ebms-sed-ws.war"  "$WILDFLY_HOME/standalone/deployments/"
cp "$SED_BUNDLE/deployments/ebms-sed-webgui.war"  "$WILDFLY_HOME/standalone/deployments/"
cp "$SED_BUNDLE/deployments/plugin-zpp.war"  "$WILDFLY_HOME/standalone/deployments/"

if [ "$INIT" = "TRUE" ]; then

	# set fix for module org.apache.ws.security
	cp "$SED_BUNDLE/modules/org.apache.ws.securitymodule.xml" "$WILDFLY_HOME/modules/system/layers/base/org/apache/ws/security/main/module.xml"


	# copy start scripts
	cp "$SED_BUNDLE/widlfly-10/start-sed.sh" "$WILDFLY_HOME/bin/"
	chmod u+x "$WILDFLY_HOME/bin/start-sed.sh"

	# create home folder
	mkdir -p  "$SED_HOME"
	cp -r "$SED_BUNDLE/sed-home" "$SED_HOME"
	
	# copy configuration
	cp "$SED_BUNDLE/widlfly-10/config/sed-roles.properties" "$WILDFLY_HOME/standalone/configuration/"
	cp "$SED_BUNDLE/widlfly-10/config/sed-users.properties" "$WILDFLY_HOME/standalone/configuration/"
	cp "$SED_BUNDLE/widlfly-10/config/standalone-ebms.xml" "$WILDFLY_HOME/standalone/configuration/"

fi

