# Money-Manager (backend)
The main purpose of the application is to manage income, expenses and savings. 

It is a stateless backend REST API with oAuth2 authorization.

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


