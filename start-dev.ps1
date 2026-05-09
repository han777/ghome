# Genesis Apartment Management System - Development Startup Script

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$($env:JAVA_HOME)\bin;$($env:Path)"

Write-Host "Starting Genesis Apartment Management System in Development Mode..." -ForegroundColor Cyan
Write-Host "Using JAVA_HOME: $($env:JAVA_HOME)" -ForegroundColor Gray

# 1. Start Backend
Write-Host "Starting Backend (Spring Boot)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd apartment-backend; mvn spring-boot:run" -WindowStyle Normal

# 2. Start Frontend
Write-Host "Starting Frontend (Vite/Vue)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd apartment-frontend; npm install; npm run dev" -WindowStyle Normal

Write-Host "Both services are starting in separate windows." -ForegroundColor Cyan
Write-Host "   - Backend: http://localhost:10080"
Write-Host "   - Frontend: http://localhost:3003"
