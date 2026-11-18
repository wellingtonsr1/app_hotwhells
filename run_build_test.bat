@echo off
set "JAVA_HOME=%LOCALAPPDATA%\Android\Jdk\jdk-17.0.21+9"
cd /d "D:\IA\app_hotwhells"
call gradlew.bat :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest
