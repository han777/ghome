# Genesis Apartment - Cloud Deployment Script
# This script SSH to cloud host and runs deployment

Write-Host "=== Genesis Apartment Cloud Deployment ===" -ForegroundColor Cyan
Write-Host "Connecting to cloud host 101.32.240.27..." -ForegroundColor Yellow

# Execute deployment on cloud host
ssh root@101.32.240.27 "cd /opt/apartment && git pull origin main && bash deploy/deploy.sh"

Write-Host "Deployment script executed." -ForegroundColor Green