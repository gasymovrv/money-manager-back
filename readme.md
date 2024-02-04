# Money-Manager
The main purpose of the application is to manage income, expenses and savings. 
[Demo](https://gasymovrv-money-manager.herokuapp.com/)

## Backend
+ Java 17, Spring Boot 2.6.2
+ OAuth2 authentication with Google or VKontakte by JWT
+ Application has made as a stateless RESTful API
+ Built frontend is in jar resources, single html page will be opened by Spring MVC (spring-boot-starter-web)
+ There are 2 mode:
  + server app - deployable from GitHub application, user data stores in dedicated postgresql database
  + desktop app - installable Windows application, user data stores in the H2 database that in the installed directory

## Frontend
+ TypeScript, React 17.0.2, Material-UI 4.11.1, Redux
+ No class components, only functional ones with React Hooks
+ Using Material-UI components for styling
+ Using Redux to store global state in memory and some custom UI configurations in local storage

## Instructions
### Build and run
+ Change GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in .env (or as environment variables) to actual
+ For local build you need Java 17, Maven 3+. NodeJS v14.16.0 and NPM 6.14.11 are only needed for frontend developing
+ Call `mvn clean install` at root of the project to build the application. `-Drevision=1.0.0` can be added to change the version of the project
+ Built result (jar file) will be in 'target' directory at root of the project
+ Call `java -Duser.timezone=UTC -jar money-manager-<version>.jar` to start the application
+ Optionally. It is possibly to build exe installer for Windows (desktop mode), just run pack_exe_installer.bat after maven build. Before generating the jar file, all required environment variables must be in the .env file
+ Optionally. Call `npm start` at frontend directory to start frontend dev server

### Switch to PostgreSQL
Instead of H2 database, you can switch to PostgreSQL. To do this, run the application with the following env variables (specify your PostgreSQL credentials):
```
USE_POSTGRES=true
SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/moneymanagerdb"
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
```

