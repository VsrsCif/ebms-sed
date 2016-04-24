@echo off
@if not "%ECHO%" == ""  echo %ECHO%

set "WILDFLY_HOME="
set "SED_BUNDLE="
set "SED_HOME="
set "INIT=false"

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
	  
	  if ["%~1"]==["-b"] (
	    shift
		set "SED_BUNDLE=%~2"
		echo SED_BUNDLE = "%SED_BUNDLE%".
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
	set "RESOLVED_SED_BUNDLE=%CD%"
popd


if "x%WILDFLY_HOME%" == "x" (
  echo ERROR: WILDFLY_HOME folder is not setted!
  goto :quit
)

if "x%SED_BUNDLE%" == "x" (
	set "SED_BUNDLE=%RESOLVED_SED_BUNDLE%"
)

if "x%SED_HOME%" == "x" (
  set  "SED_HOME=%WILDFLY_HOME%\standalone\data\"
)


echo *******************************.
echo WILDFLY_HOME = "%WILDFLY_HOME%".
echo SED_BUNDLE = "%SED_BUNDLE%".
echo SED_HOME = "%SED_HOME%".
echo INIT = "%INIT%".




rem  create module folder
if not exist %WILDFLY_HOME%\modules\org\sed\main\ (
	md  "%WILDFLY_HOME%\modules\org\sed\main\"
)
	  

rem  copy module libraries
copy "%SED_BUNDLE%\modules\ebms-msh-xsd-1.0.jar" "%WILDFLY_HOME%\modules\org\sed\main\"
copy "%SED_BUNDLE%\modules\ebms-sed-wsdl-1.0.jar" "%WILDFLY_HOME%\modules\org\sed\main\"
copy "%SED_BUNDLE%\modules\ebms-sed-commons-1.0.jar" "%WILDFLY_HOME%\modules\org\sed\main\"
rem  copy module descriptor
copy "%SED_BUNDLE%\modules\org.sed.module.xml" "%WILDFLY_HOME%\modules\org\sed\main\module.xml"

rem  deploy commons ejbs
copy "%SED_BUNDLE%\deployments\ebms-sed-dao.jar"  "%WILDFLY_HOME%\standalone\deployments\"
copy "%SED_BUNDLE%\deployments\sed-basic-tasks.jar"  "%WILDFLY_HOME%\standalone\deployments\"
rem  deploy modules 
copy "%SED_BUNDLE%\deployments\ebms-msh.ear"  "%WILDFLY_HOME%\standalone\deployments\"
copy "%SED_BUNDLE%\deployments\ebms-sed-ws.war"  "%WILDFLY_HOME%\standalone\deployments\"
copy "%SED_BUNDLE%\deployments\ebms-sed-webgui.war"  "%WILDFLY_HOME%\standalone\deployments\"
copy "%SED_BUNDLE%\deployments\plugin-zpp.war"  "%WILDFLY_HOME%\standalone\deployments\"


if "%INIT%" == "true" (
	rem  set fix for module org.apache.ws.security
	copy "%SED_BUNDLE%\modules\org.apache.ws.securitymodule.xml" "%WILDFLY_HOME%\modules\system\layers\base\org\apache\ws\security\main\module.xml"
	echo copy configuration to "%WILDFLY_HOME%\standalone\configuration\".
	rem  copy configuration
	copy "%SED_BUNDLE%\widlfly-10\config\sed-roles.properties" "%WILDFLY_HOME%\standalone\configuration\"
	copy "%SED_BUNDLE%\widlfly-10\config\sed-users.properties" "%WILDFLY_HOME%\standalone\configuration\"
	copy "%SED_BUNDLE%\widlfly-10\config\standalone-ebms.xml" "%WILDFLY_HOME%\standalone\configuration\"

	rem  copy start scripts
	echo copy start scripts "%WILDFLY_HOME%\bin\
	copy "%SED_BUNDLE%\widlfly-10\start-sed.bat" "%WILDFLY_HOME%\bin\"

	echo copy init sed-home to "%SED_HOME%\sed-home".
	rem  create home folder
	md "%SED_HOME%\sed-home"
	xcopy "%SED_BUNDLE%\sed-home" "%SED_HOME%\sed-home" /S /E
)
goto :END


:quit
echo.
echo Usage:
echo deploy-sed.bat --init -b [SED_BUNDLE] -w [WILDFLY_HOME] -s [SED_HOME]
echo.
echo   --init  initialize sed-home and wildfly properties. 
echo   -w   WILDFLY_HOME -  path jboss home: ex.: c:\temp\wildfly-10.0.0.Final\.
echo   -b   SED_BUNDLE   - path to unziped ebms-sed bundle if not given parent script folder is setted.
echo   -s   SED_HOME     - path tom application home folder  (sed.home) if is not given and --init is setted than '[WILDFLY_HOME]\standalone\data\' is setted.	


:END
pause
