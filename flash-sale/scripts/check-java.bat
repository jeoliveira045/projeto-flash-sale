@echo off
setlocal
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"
where java >nul 2>nul
if errorlevel 1 (
    echo ERRO: Java nao encontrado. Configure JAVA_HOME para um JDK 21.
    exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
    echo ERRO: JDK nao encontrado; somente JRE nao basta.
    exit /b 1
)
java -version 2>&1 | findstr /C:"21." >nul
if errorlevel 1 (
    echo ERRO: este projeto requer JDK 21. Java encontrado:
    java -version
    exit /b 1
)
javac -version 2>&1 | findstr /C:"21." >nul
if errorlevel 1 (
    echo ERRO: javac nao pertence ao JDK 21 indicado.
    javac -version
    exit /b 1
)
echo JDK 21 encontrado:
java -version
exit /b 0