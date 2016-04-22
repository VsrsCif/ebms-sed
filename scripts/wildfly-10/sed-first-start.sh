#!/bin/sh

SED_HOME=$1
#org.hibernate.dialect.H2Dialect
./standalone.sh -c standalone-ebms.xml -Dsed.home="$SED_HOME/" -Dorg.sed.msh.hibernate.hbm2ddl.auto=create -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.H2Dialect 
-Dorg.sed.init.lookups=$SED_HOME/sed-settings.xml




