# Money-Manager (backend)
The main purpose of the application is to manage income, expenses and savings. 

Application is deployed at https://money-manager.ddns.net.

It is a stateless backend REST API with oAuth2 authorization. Here is a frontend part: https://github.com/gasymovrv/money-manager-front

User data stores in PostgreSQL database.

Core technologies:
+ Java 21
+ Spring Boot 3.2.2
+ PostgreSQL 16.1
+ Maven

## Excel import and export
Application supports import and export to Excel files.
File structure must be like this:

Columns
+ 1: Date;
+ [2, n]: Incomes (first row has group of this columns named "Incomes"), n-th column is always "Incomes sum". Each column except n is a category of income;
+ [n+1, m]: Expenses (first row has group of this columns named "Expenses "), m-th column is always "Expenses sum". Each column except m is a category of expense;
+ m+1: Savings.

Rows
+ 1,2: Headers with names of columns;
+ 3: "Previous savings". This row used only when we need to provide savings saved before the table existing. Usually doesn't have values
+ 4 - end of list: incomes, expenses and savings by dates

Data grouped by years in separate lists

## Telegram Bot Integration
Application provides Telegram bot integration for managing finances directly from Telegram:

### Features
- **Account Selection**: Link your Telegram account and select which financial account to work with
- **Add Expenses/Incomes**: Quick expense and income entry via bot commands
- **Financial Reports**: Generate and receive detailed financial reports with charts

### Bot Commands
- `/selectAccount` - Select or switch between your accounts
- `/addExpense` - Add a new expense (interactive category selection)
- `/addIncome` - Add a new income (interactive category selection)
- `/report` - Generate financial report for a date range

### Report Generation
The bot can generate comprehensive financial reports including:
- **Monthly aggregated data**: Bar charts showing monthly expenses and incomes
- **Expense breakdown**: Pie charts by category
- **Income breakdown**: Pie charts by category
- **Category filtering**: Exclude specific categories from reports
- **Async processing**: Reports are queued and processed with retry logic
- **Virtual threads**: Uses Java 21 virtual threads for efficient parallel processing

### Technical Details
- Webhook-based integration for real-time message processing
- Stateful conversation management for interactive commands
- Pessimistic locking to prevent race conditions
- Idempotent message processing
- Retry logic with exponential backoff for API calls
- Scheduled cleanup of old tasks

### Telegram Bot Setup
1. Create a new bot on Telegram using [@BotFather](https://t.me/BotFather) and get the bot token (TELEGRAM_BOT_TOKEN env)
2. Set the webhook URL to the bot's webhook endpoint with secret_token (TELEGRAM_WEBHOOK_SECRET env).

   You can use curl for this:
   ```bash
   curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
      -H "Content-Type: application/json" \
      -d '{ 
        "url": "https://money-manager.ddns.net/api/telegram/webhook",
        "secret_token": "<YOUR_SECRET_TOKEN>"
      }'
   ```
   Secret token is used to verify the authenticity of the webhook request, can be generated like this:
   ```bash
   openssl rand -base64 32
   ```
3. Set Money Manager domain using [@BotFather](https://t.me/BotFather) command `/setdomain`.

    Choose Money Manager bot and set domain to `money-manager.ddns.net`

## Security
OAuth2 authentication through Google or VKontakte

Implemented by spring-boot-starter-oauth2-client but customized to remove sessions and make the application stateless RESTful API with JWT token

Schema:
+ Frontend requests this backend API like this: http://localhost:8080/oauth2/authorize/google?redirect_uri=http://localhost:3000/oauth2/redirect
+ Backend redirects frontend to chosen provider (Google or VK) login page with specific provider credentials (client_id, client_secret, etc.) and redirect_uri param which provider will use to send response to the backend. Example with Google:
  + https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=CLIENT_ID&scope=email+profile&state=STATE&redirect_uri=http://localhost:8080/oauth2/callback/google
+ User sees his login page, accept authentication of our app or not
+ Backend handles response from provider on http://localhost:8080/oauth2/callback/PROVIDER
  + if it is successfully authenticated then redirects back to the frontend with new JWT token like this: http://localhost:3000/oauth2/redirect?token=TOKEN. [Oauth2AuthenticationSuccessHandler](src/main/java/ru/rgasymov/moneymanager/security/oauth2/Oauth2AuthenticationSuccessHandler.java)
  + If it is failed then like this: http://localhost:3000/oauth2/redirect?error=error. [Oauth2AuthenticationFailureHandler](src/main/java/ru/rgasymov/moneymanager/security/oauth2/Oauth2AuthenticationFailureHandler.java)

See details in [HttpCookieOauth2AuthorizationRequestRepository.java](src/main/java/ru/rgasymov/moneymanager/security/oauth2/HttpCookieOauth2AuthorizationRequestRepository.java)

## HTTPS Setup
The application uses Nginx as a reverse proxy to handle HTTPS traffic. The setup includes:

### Architecture
- Nginx handles SSL termination and routes traffic to appropriate services
- Frontend and backend services are only accessible through Nginx
- All services communicate through Docker network using internal ports

### Configuration
1. SSL Certificates:
   ```bash
   # Generate self-signed certificates for development
   mkdir -p nginx/ssl
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout nginx/ssl/key.pem \
     -out nginx/ssl/cert.pem
   ```

2. Nginx Configuration:
   - Located in `nginx/nginx.conf`
   - Handles SSL termination
   - Routes traffic:
     - Frontend: `https://localhost/`
     - Backend API: `https://localhost/api/`
     - OAuth2 endpoints: `https://localhost/oauth2/`
   - Redirects HTTP to HTTPS
   - Properly handles OAuth2 redirects

3. Docker Compose:
   - Nginx service exposes ports 80 and 443
   - Frontend and backend services use internal ports
   - All services connected through Docker network

### Access Points
- Frontend: `https://localhost`
- Backend API: `https://localhost/api`
- OAuth2 endpoints: `https://localhost/oauth2`

Note: For production, replace self-signed certificates with proper SSL certificates from a trusted certificate authority.

## Deployment to VPS

The application is deployed at https://money-manager.ddns.net (let's say it is DNS configured for IP 1.2.3.4).

### Build Artifacts

1. **Build backend locally**:
   ```bash
   mvn clean package -Drevision=2.0.0
   ```

Note: frontend does not need this step as it will be built during Docker image creation (see next step)

2. **Build Docker images** from backend root (first update `REACT_APP_BACKEND_HOST` in `.env` for money-manager-frontend, as this image has build-time variables):
   ```bash
   docker-compose build
   ```

### VPS Setup

1. **Create project directory and generate TLS certificate + key** (or use [Certbot](readme#TLS-Certificates-via-Certbot-Lets-Encrypt)):
   ```bash
   mkdir -p money-manager/nginx/ssl
   openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
     -keyout nginx/ssl/key.pem \
     -out nginx/ssl/cert.pem
   ```

2. **Send docker-compose.yml and .env from Windows via PowerShell or Linux via SSH**:
   ```bash
   scp docker-compose.yml .env gasymovrv@1.2.3.4:~/money-manager/
   ```
Note: Don't forget to set credentials in `.env` file

3. **Package and send Docker images from WSL (or Git Bash)**:
   ```bash
   cd ~
   docker save -o money-manager-nginx.tar money-manager-nginx:2.0.0
   docker save -o money-manager-backend.tar money-manager-backend:2.0.0
   docker save -o money-manager-frontend.tar money-manager-frontend:2.0.0
   scp money-manager-nginx.tar money-manager-backend.tar money-manager-frontend.tar gasymovrv@1.2.3.4:~/money-manager
   ```

4. **Load images on VPS**:
   ```bash
   cd ~/money-manager
   docker load -i money-manager-nginx.tar
   docker load -i money-manager-backend.tar
   docker load -i money-manager-frontend.tar
   ```

5. **Start Docker containers**:
   ```bash
   docker-compose up -d
   ```

### Install Docker Compose (if needed)
```bash
# Check version on official Docker website
sudo curl -L "https://github.com/docker/compose/releases/download/v2.36.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### View Container Logs
```bash
docker logs money-manager-backend
```

### Complete Cleanup (if needed)
```bash
docker rm -f money-manager-nginx money-manager-backend money-manager-frontend money-manager-postgres
docker network rm money-manager-network
docker volume rm money-manager-postgres-data

docker rmi money-manager-nginx:2.0.0 money-manager-backend:2.0.0 money-manager-frontend:2.0.0
```

### Firewall Configuration (Optional)
```bash
# Check firewall status
sudo ufw status verbose
sudo ufw status numbered

# Allow SSH if not already done
sudo ufw allow 22/tcp

# Enable UFW (if not already enabled)
sudo ufw enable
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw default deny incoming

# Remove/deny ports
sudo ufw delete <number>
sudo ufw deny <port>/tcp

# Check open ports on VPS
sudo ss -tuln
```

### TLS Certificates via Certbot (Let's Encrypt)

First issuance creates certificate for 3 months:
```bash
sudo certbot certonly --standalone -d money-manager.ddns.net
```

Certbot automatically renews certificates and stores them in `/etc/letsencrypt/live/money-manager.ddns.net` (first time it did not auto-renew, may need manual renewal).

**Copy certificates to project directory and restart container**:
```bash
cp /etc/letsencrypt/live/money-manager.ddns.net/fullchain.pem ./money-manager/nginx/ssl/cert.pem
cp /etc/letsencrypt/live/money-manager.ddns.net/privkey.pem ./money-manager/nginx/ssl/key.pem
docker-compose restart money-manager-nginx
```

### Access PostgreSQL
```bash
docker exec -it money-manager-postgres psql -U mmpguser -d moneymanagerdb

# Then you can use SQL. Change password for example:
ALTER ROLE mmpguser WITH PASSWORD 'pass';
```

## Instructions
### Build and run
+ Run PostgreSQL by [docker-compose.yml](docker-compose.yml)
+ Change GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET and other credentials in .env (or as environment variables) to actual
+ Call `mvn clean package` at root of the project to build the application. `-Drevision=1.0.0` can be added to change the version of the project
+ Built result (jar file) will be in 'target' directory at the root of the project
+ Call `java -Duser.timezone=UTC -jar money-manager-<version>.jar` to start the application

### Docker Compose Commands
```bash
# Build and start all services
docker-compose up --build -d

# Build and start specific service
docker-compose up --build -d money-manager-frontend

# Build specific service
docker-compose build money-manager-frontend

# Just start all services without building
docker-compose up -d
```


### Local Testing with Nginx and Ngrok

**Important:** Telegram Login Widget requires the page to be served from the exact domain you set in BotFather. Using `localhost:3000` won't work due to CSP restrictions.


#### Setup:

This setup uses Nginx in Docker to proxy both frontend and backend through one domain.

1. **Start Nginx via Docker Compose:**
   ```bash
   cd money-manager-back
   docker-compose -f docker-compose-local.yml up -d
   ```
   Nginx will proxy requests from http://mm.localtest.me to your local ports. See Nginx configuration in [nginx-local config](nginx-local/nginx.conf)

2. Use Ngrok to expose your backend:
   ```bash
   ngrok http http://mm.localtest.me
   ```
   You'll get a URL like: `https://abc123.ngrok-free.dev`

3. **Configure environment variables:**

   `money-manager-back/.env`:
   ```env
   TELEGRAM_BOT_TOKEN=your-bot-token-here
   ALLOWED_ORIGINS=https://abc123.ngrok-free.dev
   ```

   `money-manager-front/.env`:
   ```env
   REACT_APP_BACKEND_HOST=https://abc123.ngrok-free.dev
   REACT_APP_TELEGRAM_BOT_USERNAME=your-bot-username
   ```
   
    Don't forget add Ngrok url as redirect_uri and allowed domain to OAuth providers (VK and Google)

4. **Start backend in IDE:**
   ```bash
   cd money-manager-back
   mvn spring-boot:run
   ```
   Backend will run on `localhost:8080`

5. **Start frontend in IDE:**
   ```bash
   cd money-manager-front
   npm start
   ```
   Frontend will run on `localhost:3000`

6. **Set domain in BotFather:**
   - Open @BotFather in Telegram
   - Send `/setdomain`
   - Select your bot
   - Enter: `abc123.ngrok-free.dev`

7. **Set up webhook for receiving messages:**

   Set webhook:
   ```bash
   curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
   -H "Content-Type: application/json" \
   -d '{
    "url": "https://abc123.ngrok-free.dev/api/telegram/webhook",
    "secret_token": "your-generated-secret-here"
   }'
   ```

8. **Open application:**
   Open in browser: `https://abc123.ngrok-free.dev`

9. **Test Login Widget:**
   - Login to Money Manager
   - Click Telegram widget in header
   - Authenticate with Telegram
   - Check backend logs for successful account linking
   - Send a message to your bot in Telegram and check backend logs.

#### Stop Nginx:
```bash
cd money-manager-back
docker-compose -f docker-compose-local.yml down
```
