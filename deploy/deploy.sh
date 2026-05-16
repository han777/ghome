#!/bin/bash
# Genesis Apartment Management System - Deployment Script
# Run this on the cloud host (101.32.240.27)
# Deployment method: git pull from GitHub → build on server → deploy

set -e

# === Configuration ===
PROJECT_DIR="/opt/apartment"
REPO_URL="ssh://git@ssh.github.com:443/han777/ghome.git"
BRANCH="main"
BACKEND_DIR="$PROJECT_DIR/apartment-backend"
FRONTEND_DIR="$PROJECT_DIR/apartment-frontend"
DEPLOY_DIR="$PROJECT_DIR/deploy"
NGINX_CONF="/etc/nginx/conf.d/ghome.conf"
BACKEND_JAR="$BACKEND_DIR/target/apartment-backend-0.0.1-SNAPSHOT.jar"
JAVA_HOME="/usr/lib/jvm/java-21-konajdk-21.0.10-1.oc9"

echo "=== Genesis Apartment Deployment ==="
echo "Method: git pull from GitHub + build on server"
echo ""

# === Step 1: Clone or pull source code from GitHub ===
echo "[1/7] Pulling source code from GitHub..."
export JAVA_HOME="$JAVA_HOME"

if [ -d "$PROJECT_DIR/.git" ]; then
    # Repo already cloned, pull latest
    cd "$PROJECT_DIR"
    git pull origin "$BRANCH" || { echo "ERROR: git pull failed. Check SSH deploy key."; exit 1; }
    echo "Source code updated."
else
    # First time: clone the repo
    mkdir -p "$PROJECT_DIR"
    git clone -b "$BRANCH" "$REPO_URL" "$PROJECT_DIR" || { echo "ERROR: git clone failed. Check SSH deploy key and network."; exit 1; }
    echo "Source code cloned."
fi

# === Step 2: Build backend (Spring Boot) ===
echo "[2/7] Building backend (Spring Boot)..."
cd "$BACKEND_DIR"
mvn clean package -DskipTests || { echo "ERROR: Backend build failed."; exit 1; }
echo "Backend built: $BACKEND_JAR"

# === Step 3: Inject production config into jar ===
echo "[3/7] Injecting production config..."
cd "$BACKEND_DIR/target"
mkdir -p temp/BOOT-INF/classes
cd temp && jar xf ../apartment-backend-0.0.1-SNAPSHOT.jar BOOT-INF/classes/application.yml

# Read production DB credentials from .env.deploy on server
if [ -f "$PROJECT_DIR/.env.deploy" ]; then
    # Parse .env.deploy for production values
    DB_HOST=$(grep '^DB_HOST=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    DB_PORT=$(grep '^DB_PORT=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    DB_NAME=$(grep '^DB_NAME=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    DB_USER=$(grep '^DB_USER=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    DB_PASSWORD=$(grep '^DB_PASSWORD=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    WECOM_CORPID=$(grep '^WECOM_CORPID=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    WECOM_SECRET=$(grep '^WECOM_SECRET=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    WECOM_AGENTID=$(grep '^WECOM_AGENTID=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
    WECOM_CALLBACK_BASE_URL=$(grep '^WECOM_CALLBACK_BASE_URL=' "$PROJECT_DIR/.env.deploy" | cut -d= -f2)
else
    echo "WARNING: .env.deploy not found. Using dev config (NOT recommended for production)."
fi

# Update application.yml with production values using sed
sed -i "s|url: jdbc:postgresql://.*|url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}|g" BOOT-INF/classes/application.yml
sed -i "s|username: .*|username: ${DB_USER}|g" BOOT-INF/classes/application.yml
sed -i "s|password: .*|password: ${DB_PASSWORD}|g" BOOT-INF/classes/application.yml
sed -i "s|corpid: .*|corpid: ${WECOM_CORPID}|g" BOOT-INF/classes/application.yml
sed -i "s|secret: .*|secret: ${WECOM_SECRET}|g" BOOT-INF/classes/application.yml
sed -i "s|agentid: .*|agentid: ${WECOM_AGENTID}|g" BOOT-INF/classes/application.yml
sed -i "s|    base-url: .*|    base-url: ${WECOM_CALLBACK_BASE_URL}|g" BOOT-INF/classes/application.yml

# Repack jar with updated config
jar uf ../apartment-backend-0.0.1-SNAPSHOT.jar BOOT-INF/classes/application.yml
rm -rf temp
echo "Production config injected into jar."

# === Step 4: Build frontend (Vue 3 + Vite) ===
echo "[4/7] Building frontend (Vue/Vite)..."
cd "$FRONTEND_DIR"
npm install --prefer-offline || npm install || { echo "ERROR: npm install failed."; exit 1; }
npx vite build || { echo "ERROR: Frontend build failed."; exit 1; }
echo "Frontend built at: $FRONTEND_DIR/dist"

# === Step 5: Deploy frontend static files ===
echo "[5/7] Deploying frontend..."
rm -rf /var/www/ghome
cp -r "$FRONTEND_DIR/dist" /var/www/ghome
echo "Frontend deployed to /var/www/ghome"

# === Step 6: Database schema updates (if needed) ===
echo "[6/7] Checking database schema..."
# This step is manual - run ALTER TABLE statements if new JPA entity columns added
# Example: psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "ALTER TABLE ... ADD COLUMN IF NOT EXISTS ..."
echo "Skip automatic schema migration. Run manually if JPA entities changed."

# === Step 7: Ensure upload directory + update configs + restart services ===
echo "[7/7] Restarting services..."
mkdir -p /opt/uploads
cp "$DEPLOY_DIR/nginx.conf" /etc/nginx/conf.d/ghome.conf
cp "$DEPLOY_DIR/apartment-backend.service" /etc/systemd/system/apartment-backend.service
systemctl daemon-reload
systemctl restart apartment-backend
nginx -t && nginx -s reload || { echo "WARNING: nginx reload failed."; }
echo "Services restarted."

# === Verify ===
echo ""
echo "=== Verifying deployment ==="
sleep 3

# Backend API check
BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:10080/api/auth/wecom/authorize)
if [ "$BACKEND_STATUS" = "200" ]; then
    echo "✓ Backend API is running (HTTP $BACKEND_STATUS)"
else
    echo "✗ Backend API check returned HTTP $BACKEND_STATUS (may need more time to start)"
fi

# Frontend check
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://ghome.genesismedtech.com/)
if [ "$FRONTEND_STATUS" = "200" ]; then
    echo "✓ Frontend is accessible (HTTP $FRONTEND_STATUS)"
else
    echo "✗ Frontend check returned HTTP $FRONTEND_STATUS"
fi

echo ""
echo "=== Deployment Complete ==="
echo "Backend: http://ghome.genesismedtech.com/api/"
echo "Frontend: http://ghome.genesismedtech.com/"
echo "WeChat Work Callback: http://ghome.genesismedtech.com/api/auth/wecom/callback"