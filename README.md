# Steps To Setup Business Tier (REST Api)

**Note:** 
1. Make sure the MongoDb database is up and running on port **27017**. Please refer to the DataTier setup steps @ https://github.com/avik-fse/fse-data-tier/blob/master/README.md.

## Steps
1. Get the source code from git repo:  
   `git clone https://github.com/avik-fse/task-manager-api.git`

2. From command prompt, go to the root folder of the application in the checked out location (i.e inside task-manager-api):  
   
   a. To build the package and run the test cases use the command:  
    `mvn clean package`
   
### Steps to run locally as Spring Boot standalone(embeded tomcat) application:
   
   a. To run the API service use the command(from the root application folder in command prompt):  
    `mvn spring-boot:run`

### Steps to run the api in external server(tomcat):
   
   a. From the root application folder in command prompt, execute the below command to create the war file:  
    `mvn clean package`
    
   b. copy the **task-manager-api.war** file from the `target` directory and deply it in server

_Note: If you are using any IDE to open the source code and compile, then you might need to add lombok suppport to your IDE
and also enable annotation processing in the IDE._

_How to setup lombok for Intellij:_  
https://projectlombok.org/setup/intellij  

_How to setup lombok for eclipse:_  
https://projectlombok.org/setup/eclipse  

