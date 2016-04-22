#/!bin/sh

EbmsSeDFolder=$1
TmpFolder="ebms-sed-$(date +%Y%m%d%H%M)"
rm -rf $TmpFolder

mkdir $TmpFolder
mkdir "$TmpFolder/modules"
mkdir "$TmpFolder/deployments"
mkdir "$TmpFolder/widlfly-10"

cp "$EbmsSeDFolder/ebms-sed-libs/ebms-msh-xsd/target/ebms-msh-xsd-1.0.jar" "$TmpFolder/modules/" 	
cp "$EbmsSeDFolder/ebms-sed-libs/ebms-sed-wsdl/target/ebms-sed-wsdl-1.0.jar" "$TmpFolder/modules/"
cp "$EbmsSeDFolder/ebms-sed-libs/ebms-sed-commons/target/ebms-sed-commons-1.0.jar" "$TmpFolder/modules/"
cp "$EbmsSeDFolder/scripts/wildfly-10/modules/org.sed.module.xml" "$TmpFolder/modules/"
cp "$EbmsSeDFolder/scripts/wildfly-10/modules/org.apache.ws.securitymodule.xml" "$TmpFolder/modules/"

# commons ejbs
cp "$EbmsSeDFolder/ebms-sed-dao/target/ebms-sed-dao.jar" "$TmpFolder/deployments/"
cp "$EbmsSeDFolder/ebms-sed-tasks/sed-basic-tasks/target/sed-basic-tasks.jar" "$TmpFolder/deployments/"
# modules
cp "$EbmsSeDFolder/ebms-msh-module/ebms-msh-ear/target/ebms-msh.ear" "$TmpFolder/deployments/"
cp "$EbmsSeDFolder/ebms-sed-module/ebms-sed-ws/target/ebms-sed-ws.war"  "$TmpFolder/deployments/"
cp "$EbmsSeDFolder/ebms-sed-module/ebms-sed-web/target/ebms-sed-webgui.war"  "$TmpFolder/deployments/"
cp "$EbmsSeDFolder/ebms-sed-plugins/ebms-sed-zpp-plugin/target/plugin-zpp.war"  "$TmpFolder/deployments/"
# configuration file
cp -r "$EbmsSeDFolder/scripts/wildfly-10/config"  "$TmpFolder/widlfly-10"
# deploy script
cp "$EbmsSeDFolder/scripts/wildfly-10/first-deploy.sh" "$TmpFolder/widlfly-10"
cp "$EbmsSeDFolder/scripts/wildfly-10/sed-first-start.sh" "$TmpFolder/widlfly-10"

# init data:
cp -r "$EbmsSeDFolder/scripts/init-sample" "$TmpFolder/sed-home"

zip -r "$TmpFolder.zip" $TmpFolder

rm -rf $TmpFolder



#./standalone.sh -c standalone-ebms.xml -Dsed.home=/opt/servers/wildfly-10.0.0.Final/sed-home -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect


#./standalone.sh -c standalone-ebms.xml -Dsed.home=/opt/servers/wildfly-10.0.0.Final/sed-home -Dorg.sed.msh.hibernate.hbm2ddl.auto=create -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect -Dorg.sed.msh.sender.workers.count=7 -Dorg.sed.init.lookups=sed-settings.xml

#org.hibernate.dialect.H2Dialect
#./standalone.sh -c standalone-ebms.xml -Dsed.home=/opt/servers/wildfly-10.0.0.Final/sed-home -Dorg.sed.msh.hibernate.hbm2ddl.auto=create -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.H2Dialect -Dorg.sed.msh.sender.workers.count=7




