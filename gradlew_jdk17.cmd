@echo off
set "JAVA_HOME=C:\Users\wellingtonrodrigues\AppData\Local\Android\Jdk\jdk-17.0.21+9"
set "JAVACMD=%JAVA_HOME%\bin\java.exe"
set "JAVACMD_EXISTS="
if exist "%JAVACMD%" set "JAVACMD_EXISTS=yes"

REM Export for subprocesses
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Gradle wrapper: download and execute using the JDK 17
set GDLIB=%LOCALAPPDATA%\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\lib
set GRADLE_CACHE=%LOCALAPPDATA%\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c

REM Find the distribution zip/settler to extract classpath
set "GRADLE_CP="
for /r "%GRADLE_CACHE%\" %%F in (*.jar) do (
  if not "%%~nxF"=="gradle-wrapper.jar" if not "%%~nxF"=="gradle-launcher*.jar" if not "%%~nxF"=="gradle-tooling-api*.jar" (
    if "!GRADLE_CP!"=="" (
      set "GRADLE_CP=%%F"
    ) else (
      set "GRADLE_CP=!GRADLE_CP!;%%F"
    )
  )
)

%JAVACMD% -classpath "%GRADLE_CP%" org.gradle.launcher.GradleMain %*

exit /b %ERRORLEVEL%