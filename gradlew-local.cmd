@echo off
setlocal
pushd "%~dp0"

rem Usa el JDK local elegido para Android Studio si JAVA_HOME no esta definido.
if not defined JAVA_HOME if exist ".gradle\config.properties" (
    for /f "tokens=1,* delims==" %%A in (.gradle\config.properties) do (
        if "%%A"=="java.home" set "JAVA_HOME=%%B"
    )
)
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
)

rem NUL no admite archivos de socket: Java usa su alternativa TCP para los pipes locales.
rem Evita el fallo AF_UNIX "Invalid argument: connect" observado en esta PC con Java 17 y 21.
rem setlocal mantiene estos ajustes dentro de esta ejecucion.
set "JAVA_TOOL_OPTIONS=%JAVA_TOOL_OPTIONS% -Djdk.net.unixdomain.tmpdir=NUL"
call gradlew.bat %*
set "INTU_GRADLE_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %INTU_GRADLE_EXIT_CODE%
