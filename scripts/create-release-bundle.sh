#/!bin/sh


SED_PROJECT=$1
if [ "x$SED_PROJECT" = "x" ]; then
	SED_PROJECT="../"
fi


ZIP_FILENAME="ebms-sed-$(date +%Y%m%d_%H%M)"
rm -rf $ZIP_FILENAME

mkdir $ZIP_FILENAME
mkdir "$ZIP_FILENAME/modules"
mkdir "$ZIP_FILENAME/deployments"
mkdir "$ZIP_FILENAME/widlfly-10"

cp "$SED_PROJECT/ebms-sed-libs/ebms-msh-xsd/target/ebms-msh-xsd-1.0.jar" "$ZIP_FILENAME/modules/" 	
cp "$SED_PROJECT/ebms-sed-libs/ebms-sed-wsdl/target/ebms-sed-wsdl-1.0.jar" "$ZIP_FILENAME/modules/"
cp "$SED_PROJECT/ebms-sed-libs/ebms-sed-commons/target/ebms-sed-commons-1.0.jar" "$ZIP_FILENAME/modules/"
cp "$SED_PROJECT/scripts/wildfly-10/modules/org.sed.module.xml" "$ZIP_FILENAME/modules/"
cp "$SED_PROJECT/scripts/wildfly-10/modules/org.apache.ws.securitymodule.xml" "$ZIP_FILENAME/modules/"

# commons ejbs
cp "$SED_PROJECT/ebms-sed-dao/target/ebms-sed-dao.jar" "$ZIP_FILENAME/deployments/"
cp "$SED_PROJECT/ebms-sed-tasks/sed-basic-tasks/target/sed-basic-tasks.jar" "$ZIP_FILENAME/deployments/"
# modules
cp "$SED_PROJECT/ebms-msh-module/ebms-msh-ear/target/ebms-msh.ear" "$ZIP_FILENAME/deployments/"
cp "$SED_PROJECT/ebms-sed-module/ebms-sed-ws/target/ebms-sed-ws.war"  "$ZIP_FILENAME/deployments/"
cp "$SED_PROJECT/ebms-sed-module/ebms-sed-web/target/ebms-sed-webgui.war"  "$ZIP_FILENAME/deployments/"
cp "$SED_PROJECT/ebms-sed-plugins/ebms-sed-zpp-plugin/target/plugin-zpp.war"  "$ZIP_FILENAME/deployments/"
# configuration file
cp -r "$SED_PROJECT/scripts/wildfly-10/config"  "$ZIP_FILENAME/widlfly-10"
# deploy script
cp "$SED_PROJECT/scripts/wildfly-10/deploy-sed.sh" "$ZIP_FILENAME/widlfly-10"
cp "$SED_PROJECT/scripts/wildfly-10/start-sed.sh" "$ZIP_FILENAME/widlfly-10"
cp "$SED_PROJECT/scripts/wildfly-10/deploy-sed.bat" "$ZIP_FILENAME/widlfly-10"
cp "$SED_PROJECT/scripts/wildfly-10/start-sed.bat" "$ZIP_FILENAME/widlfly-10"

# init data:
cp -r "$SED_PROJECT/scripts/init-sample" "$ZIP_FILENAME/sed-home"

zip -r "$ZIP_FILENAME.zip" $ZIP_FILENAME

rm -rf $ZIP_FILENAME





