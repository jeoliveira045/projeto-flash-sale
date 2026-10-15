@echo off
setlocal
set "PROJECT_ROOT=%~dp0.."
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0check-java.bat"
if errorlevel 1 exit /b 1
powershell -NoProfile -Command "if ((Test-NetConnection localhost -Port 5672 -InformationLevel Quiet) -and (Test-NetConnection localhost -Port 6379 -InformationLevel Quiet)) { exit 0 }; exit 1"
if errorlevel 1 (
    echo ERRO: RabbitMQ na porta 5672 e Redis na porta 6379 devem estar ativos localmente.
    exit /b 1
)
if not exist "%PROJECT_ROOT%\checkout-service\target\checkout-service-1.0.0-SNAPSHOT.jar" (
    echo ERRO: JARs nao encontrados. Execute scripts\setup.bat primeiro.
    exit /b 1
)
if not exist "%PROJECT_ROOT%\order-processor-service\target\order-processor-service-1.0.0-SNAPSHOT.jar" (
    echo ERRO: JARs nao encontrados. Execute scripts\setup.bat primeiro.
    exit /b 1
)
start "Order Processor Service" /D "%PROJECT_ROOT%\order-processor-service" cmd /k "java -jar target\order-processor-service-1.0.0-SNAPSHOT.jar"
start "Checkout Service" /D "%PROJECT_ROOT%\checkout-service" cmd /k "java -jar target\checkout-service-1.0.0-SNAPSHOT.jar"
echo Servicos iniciados em janelas separadas.
echo Interface visual: http://localhost:8080/
echo Painel RabbitMQ: http://localhost:15672/
exit /b 0