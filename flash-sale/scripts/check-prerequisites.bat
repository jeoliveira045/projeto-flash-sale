@echo off
setlocal
call "%~dp0check-java.bat"
if errorlevel 1 exit /b 1
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERRO: Maven 3.6.3 ou superior nao foi encontrado no PATH.
    exit /b 1
)
set "MAVEN_OPTS="
for /f "tokens=3" %%V in ('mvn -version ^| findstr /B /C:"Apache Maven"') do set "MAVEN_VERSION=%%V"
for /f "tokens=1-3 delims=." %%a in ("%MAVEN_VERSION%") do (
    set "MAVEN_MAJOR=%%a"
    set "MAVEN_MINOR=%%b"
    set "MAVEN_PATCH=%%c"
)
set /a MAVEN_VERSION_CODE=%MAVEN_MAJOR%*10000+%MAVEN_MINOR%*100+%MAVEN_PATCH%
echo Maven %MAVEN_VERSION%
if %MAVEN_VERSION_CODE% LSS 30603 (
    echo ERRO: instale Maven 3.6.3 ou mais recente.
    exit /b 1
)
exit /b 0