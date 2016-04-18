rm -rf ../standalone/tmp/*
rm -rf ../standalone/data/*
rm -rf ../standalone/deployments/*.failed
rm -rf ../standalone/deployments/*.deployed

export JAVA_HOME=/opt/java/jdk1.8.0_60/


cp "/ebms-sed/ebms-sed-libs/ebms-msh-xsd/target/ebms-msh-xsd-1.0.jar" ../modules/org/sed/main/
cp "/ebms-sed/ebms-sed-libs/ebms-sed-wsdl/target/ebms-sed-wsdl-1.0.jar" ../modules/org/sed/main/
cp "ebms-sed/ebms-sed-libs/ebms-sed-commons/target/ebms-sed-commons-1.0.jar" ../modules/org/sed/main/



cp "/ebms-sed/ebms-msh-module/ebms-msh-ear/target/ebms-msh.ear" ../standalone/deployments/
cp "/ebms-sed/ebms-sed-module/ebms-sed-ws/target/ebms-sed-ws.war" ../standalone/deployments/
cp "/ebms-sed/ebms-sed-module/ebms-sed-web/target/ebms-sed-webgui.war" ../standalone/deployments/
cp "/ebms-sed/ebms-sed-plugins/ebms-sed-zpp-plugin/target/plugin-zpp.war"  ../standalone/deployments/

cp "/ebms-sed/ebms-sed-dao/target/ebms-sed-dao.jar" ../standalone/deployments/






#./standalone.sh -c standalone-ebms.xml -Dsed.home=/opt/servers/wildfly-10.0.0.Final/sed-home -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect


./standalone.sh -c standalone-ebms.xml -Dsed.home=/opt/servers/wildfly-10.0.0.Final/sed-home -Dorg.sed.msh.hibernate.hbm2ddl.auto=create -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect -Dorg.sed.msh.sender.workers.count=7


