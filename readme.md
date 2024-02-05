# Money-Manager (backend)
The main purpose of the application is to manage income, expenses and savings. 

User data stores in PostgreSQL database

+ Java 21
+ Spring Boot 3.2.2
+ PostgreSQL 16.1

### Security:
OAuth2 authentication with Google or VKontakte by JWT

Implemented by regular spring-boot-starter-oauth2-client but customized to remove sessions and make the application stateless RESTful API with JWT token

Schema:
+ Frontend requests the backend API like this: http://localhost:8080/oauth2/authorize/google?redirect_uri=http://localhost:3000/oauth2/redirect
+ Backend redirects frontend to chosen provider (Google or VK) login page with specific provider credentials (client_id, secret_id, etc.) and redirect param which provider will use to send response to the backend. Example with Google:
  + https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=CLIENT_ID&scope=email+profile&state=STATE&redirect_uri=http://localhost:8080/oauth2/callback/google
+ User sees his login page, accept authentication of our app or not
+ Backend handles response from provider on http://localhost:8080/oauth2/callback/PROVIDER
  + if it is successfully authenticated then redirects back to the frontend with new JWT token like this: http://localhost:3000/oauth2/redirect?token=TOKEN. [Oauth2AuthenticationSuccessHandler](src/main/java/ru/rgasymov/moneymanager/security/oauth2/Oauth2AuthenticationSuccessHandler.java)
  + If it is failed then like this: http://localhost:3000/oauth2/redirect?error=error. [Oauth2AuthenticationFailureHandler](src/main/java/ru/rgasymov/moneymanager/security/oauth2/Oauth2AuthenticationFailureHandler.java)

See details in [HttpCookieOauth2AuthorizationRequestRepository.java](src/main/java/ru/rgasymov/moneymanager/security/oauth2/HttpCookieOauth2AuthorizationRequestRepository.java)

## Instructions
### Build and run
+ Run PostgreSQL by [docker-compose.yml](docker-compose.yml)
+ Change GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET and other credentials in .env (or as environment variables) to actual
+ Call `mvn clean package` at root of the project to build the application. `-Drevision=1.0.0` can be added to change the version of the project
+ Built result (jar file) will be in 'target' directory at the root of the project
+ Call `java -Duser.timezone=UTC -jar money-manager-<version>.jar` to start the application


