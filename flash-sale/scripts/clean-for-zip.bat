@echo off
setlocal
set "PROJECT_ROOT=%~dp0.."
echo Esta limpeza apaga somente artefatos gerados pelo build e execucao:
echo   - pastas target dos modulos Maven
echo   - bancos H2 locais e logs dos dois servicos
echo Nao apaga fontes, scripts, configuracoes ou cache Maven do usuario.
choice /C SN /N /M "Deseja limpar agora? [S/N] "
if errorlevel 2 (
    echo Limpeza cancelada.
    exit /b 0
)
for %%D in (common-contracts checkout-service order-processor-service) do (
    if exist "%PROJECT_ROOT%\%%D\target" rmdir /S /Q "%PROJECT_ROOT%\%%D\target"
)
for %%F in ("%PROJECT_ROOT%\checkout-service\checkout-db.mv.db" "%PROJECT_ROOT%\checkout-service\checkout-db.trace.db" "%PROJECT_ROOT%\order-processor-service\processor-db.mv.db" "%PROJECT_ROOT%\order-processor-service\processor-db.trace.db") do (
    if exist "%%~F" del /Q "%%~F"
)
for %%D in (checkout-service order-processor-service) do (
    if exist "%PROJECT_ROOT%\%%D\logs" rmdir /S /Q "%PROJECT_ROOT%\%%D\logs"
)
echo Limpeza concluida. O projeto esta pronto para compactacao.
exit /b 0