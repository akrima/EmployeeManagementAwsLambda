```markdown
# Employee Management Lambda Project

This project aims to create a crud employee management system using AWS Lambda and DynamoDB.

Using tools:
    - AWS Lambdas
    - Test Containers
    - DynamoDB
    - AWS CLI
    - AWS SAM
    - AWS GateWay
    - AWS IAM
    - AWS CloudWatch

## Project Structure

EmployeeManagementLambdaJava
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── akrima
│   │   │           └── employeemanagement
│   │   │               ├── AddEmployeeLambda.java
│   │   │               ├── UpdateEmployeeLambda.java
│   │   │               ├── RetrieveEmployeeLambda.java
│   │   │               ├── RetrieveAllEmployeesLambda.java
│   │   │               ├── DeleteEmployeeLambda.java
│   │   │               └── model
│   │   │                   └── Employee.java
│   │   └── resources
│   │       └── application.properties
│   │
│   ├── test
│   │   └── java
│   │       └── com
│   │           └── akrima
│   │               └── employeemanagement
│   │                   └── integration
│   │                       └── EmployeeLambdaIntegrationTest.java
│   │
│   └── resources
│       └── test.properties
│
├── target
│   ├── EmployeeManagementLambdaJava-1.0-SNAPSHOT.jar
│   └── ...
│
├── .gitignore
├── pom.xml
└── README.md


## Configuration

- Ensure proper IAM permissions are set for your Lambda functions to access DynamoDB.
- Check and adjust configuration files `application.properties` and `log4j2.xml` if needed.

## Dependencies

- [Spring Web](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html)
- [AWS SDK](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)

## Execution

Compile the project and deploy Lambda functions to AWS trought template.yaml file:
    - [AWS SAM CLI] (https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)
    - [AWS CLI] (https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
    - Set up aws cli via : aws configure command if it's not done yet
    - Build your application: mvn clean package
    - Make sure to update template.yaml before deploying your application using SAM : sam deploy --guided
    - Create dynamodb table "Employee"
    - Add IAM write/read permissions to the lambdas
    - Then you can start testing lambdas


## Contributing


## License
