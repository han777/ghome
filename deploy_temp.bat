@echo off
echo === Genesis Apartment Cloud Deployment ===
echo Connecting to cloud host 101.32.240.27...
ssh root@101.32.240.27 "cd /opt/apartment && git pull origin main && bash deploy/deploy.sh"
echo Deployment script executed.