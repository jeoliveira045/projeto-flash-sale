@echo off
setlocal
set "PROJECT_ROOT=%~dp0.."
set "MAVEN_OPTS="
call "%~dp0check-prerequisites.bat"
if errorlevel 1 exit /b 1
pushd "%PROJECT_ROOT%"
echo Baixando dependencias do Maven Central e executando testes/build...
echo Este projeto ignora mirrors Maven locais para nao depender de Nexus corporativo.
mvn -gs "%~dp0maven-settings.xml" -s "%~dp0maven-settings.xml" -U clean package
set "BUILD_RESULT=%ERRORLEVEL%"
popd
if not "%BUILD_RESULT%"=="0" (
    echo ERRO: setup/build falhou. Confirme acesso ao Maven Central e tente novamente.
    exit /b %BUILD_RESULT%
)
echo Setup concluido. Para iniciar os servicos, execute scripts\start.bat.
exit /b 0