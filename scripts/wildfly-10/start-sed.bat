@echo off

set "WILDFLY_HOME="
set "RESOLVED_WILDFLY_HOME="
set "SED_HOME="
set "INIT=false"
set "SED_OPTS=-c standalone-ebms.xml"

:loop
      if ["%~1"]==[""] (
        echo done.
        goto endParamReading
      )
	  
	  if ["%~1"]==["--init"] (
        set "INIT=true"
      )
	  
	  if ["%~1"]==["-w"] (
	    
		set "WILDFLY_HOME=%~2"
		echo WILDFLY_HOME = "%WILDFLY_HOME%".
      )
	    
	  if ["%~1"]==["-s"] (
	    shift
		set "SED_HOME=%~2"
		echo SED_HOME = "%SED_HOME%".
      )	  
      ::--------------------------
      shift
      goto loop
:endParamReading

pushd "%CD%\.."
	set "RESOLVED_WILDFLY_HOME=%CD%"
popd


if "x%WILDFLY_HOME%" == "x" (
		set "WILDFLY_HOME=%RESOLVED_WILDFLY_HOME%"

)

if "x%SED_HOME%" == "x" (
  set  "SED_HOME=%WILDFLY_HOME%\standalone\data\sed-home"
)

set "SED_OPTS=%SED_OPTS% -Dsed.home=%SED_HOME%"



if "%INIT%" == "true" (
	
	set "SED_OPTS=%SED_OPTS% -Dorg.sed.msh.hibernate.hbm2ddl.auto=create -Dorg.sed.msh.hibernate.dialect=org.hibernate.dialect.H2Dialect -Dorg.sed.init.lookups=%SED_HOME%\sed-settings.xml"
)


echo *********************************************************************************************************************************
echo * WILDFLY_HOME =  "%WILDFLY_HOME%"
echo * SED_HOME     =  "%SED_HOME%"
echo * INIT         =  "%INIT%"
echo * SED_OPTS     =  "%SED_OPTS%"
echo *********************************************************************************************************************************

%WILDFLY_HOME%\bin\standalone.bat %SED_OPTS%

