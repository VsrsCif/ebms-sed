#!/bin/sh


quit () {
	echo "\nUsage:\n"
	echo "start-sed.bat --init -s [SED_HOME]\n"
	echo "  --init  initialize sed-home and wildfly properties. "
	echo "  -s   SED_HOME     - path tom application home folder  (sed.home) if is not given and --init is setted than '[WILDFLY_HOME]\standalone\data\' is setted.	"
        exit
}


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
    --init)
      INIT="TRUE"
    ;;
    *)
      # unknown option
    ;;
  esac
  shift # past argument or value
done




DIRNAME=`dirname "$0"`
RESOLVED_WILDFLY_HOME=`cd "$DIRNAME/.." >/dev/null; pwd`
if [ "x$WILDFLY_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    WILDFLY_HOME=$RESOLVED_WILDFLY_HOME
else
 SANITIZED_WILDFLY_HOME=`cd "$WILDFLY_HOME"; pwd`
 if [ "$RESOLVED_WILDFLY_HOME" != "$SANITIZED_WILDFLY_HOME" ]; then
   echo ""
   echo "   WARNING:  WILDFLY_HOME may be pointing to a different installation - unpredictable results may occur."
   echo ""
   echo "             WILDFLY_HOME: $WILDFLY_HOME"
   echo ""
   sleep 2s
 fi
fi


if [ "x$WILDFLY_HOME" = "x" ]; then
	quit;
fi

if [ ! -d "$WILDFLY_HOME" ]; then
	echo "WILDFLY_HOME folder not exists! Check parameters!"
	quit;
fi

if [ "x$SED_HOME" = "x" ]; then
	SED_HOME="$WILDFLY_HOME/standalone/data/sed-home";
fi

SED_OPTS=" -c standalone-ebms.xml -Dsed.home=$SED_HOME/";

if [ "$INIT" = "TRUE" ]; then
	SED_OPTS="$SED_OPTS -Dorg.sed.msh.hibernate.hbm2ddl.auto=create -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.H2Dialect -Dorg.sed.init.lookups=$SED_HOME/sed-settings.xml";
fi

echo "*********************************************************************************************************************************"
echo "* WILDFLY_HOME =  $WILDFLY_HOME"
echo "* SED_HOME     =  $SED_HOME"
echo "* INIT         =  $INIT"
echo "* SED_OPTS     =  $SED_OPTS"
echo "*********************************************************************************************************************************"

#org.hibernate.dialect.H2Dialect
$WILDFLY_HOME/bin/standalone.sh $SED_OPTS




