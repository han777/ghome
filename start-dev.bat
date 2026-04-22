@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Starting Genesis Apartment Management System in Development Mode...
echo Using JAVA_HOME: %JAVA_HOME%

echo Starting Backend (Spring Boot)...
start cmd /k "cd apartment-backend && mvn spring-boot:run"

echo waiting for backend finished...
pause

echo Starting Frontend (Vite/Vue)...
start cmd /k "cd apartment-frontend && npm run dev"

echo Both services are starting in separate windows.
echo    - Backend: http://localhost:8081
echo    - Frontend: http://localhost:3000
pause
