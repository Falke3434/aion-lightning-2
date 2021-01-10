@echo off
title Aion-Lightning Chat Server Console
:start
echo Starting Aion-Lightning Chat Server.
echo.
REM -------------------------------------
REM Default parameters for a basic server.
java -Xms128m -Xmx128m -ea -Xbootclasspath/p:./libs/jsr166.jar -javaagent:libs/al_commons.jar -cp ./libs/*;al_chat.jar com.aionemu.chatserver.ChatServer
REM
REM -------------------------------------

SET CLASSPATH=%OLDCLASSPATH%


if ERRORLEVEL 1 goto error
goto end
:error
echo.
echo Chat Server Terminated Abnormaly, Please Verify Your Files.
echo.
:end
echo.
echo Chat Server Terminated.
echo.
pause