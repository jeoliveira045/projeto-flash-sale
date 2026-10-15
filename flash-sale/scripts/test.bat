@echo off
setlocal
set "PROJECT_ROOT=%~dp0.."
set "MAVEN_OPTS="
call "%~dp0check-prerequisites.bat"
if errorlevel 1 exit /b 1
pushd "%PROJECT_ROOT%"
if /I "%~1"=="integracao" goto integration
echo Executando testes unitarios; Redis e RabbitMQ nao sao necessarios.
mvn -gs "%~dp0maven-settings.xml" -s "%~dp0maven-settings.xml" test
set "TEST_RESULT=%ERRORLEVEL%"
popd
exit /b %TEST_RESULT%

:integration
powershell -NoProfile -Command "if ((Test-NetConnection localhost -Port 5672 -InformationLevel Quiet) -and (Test-NetConnection localhost -Port 6379 -InformationLevel Quiet)) { exit 0 }; exit 1"
if errorlevel 1 (
    echo ERRO: inicie RabbitMQ na porta 5672 e Redis na porta 6379 antes do teste.
    popd
    exit /b 1
)
echo Executando teste ponta a ponta dos fluxos CONFIRMED e CANCELLED...
mvn -gs "%~dp0maven-settings.xml" -s "%~dp0maven-settings.xml" -pl checkout-service -am -DrunLocalIntegration=true -Dtest=CheckoutFlowIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
set "TEST_RESULT=%ERRORLEVEL%"
popd
exit /b %TEST_RESULT%