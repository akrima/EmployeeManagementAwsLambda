package com.akrima.employeemanagement.integration;

import com.akrima.employeemanagement.*;
import com.akrima.employeemanagement.model.Employee;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @Author Abderrahim KRIMA
 */

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmployeeLambdaIntegrationTest {

    private static final int DYNAMODB_PORT = 8000;
    private static final String DYNAMODB_TABLE_NAME = "Employee";

    private static GenericContainer<?> dynamoDbContainer;
    private static DynamoDbClient dynamoDbClient;

    @Mock
    private Context mockContext;
    @Mock
    private LambdaLogger lambdaLogger;

    @BeforeAll
    public static void setUp() {
        dynamoDbContainer = new GenericContainer<>("amazon/dynamodb-local:latest")
                .withExposedPorts(DYNAMODB_PORT);
        dynamoDbContainer.start();

        String serviceEndpoint = "http://" + dynamoDbContainer.getContainerIpAddress() + ":" + dynamoDbContainer.getMappedPort(DYNAMODB_PORT);

        AwsBasicCredentials credentials = AwsBasicCredentials.create("accessKey", "secretKey");

        dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create(serviceEndpoint))
                .build();

        createTable();

    }

    @AfterAll
    public static void tearDown() {
        dynamoDbContainer.stop();
    }

    private static void createTable() {
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .attributeDefinitions(AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build())
                .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
                .tableName(DYNAMODB_TABLE_NAME)
                .build();

        dynamoDbClient.createTable(createTableRequest);
    }

    @Test
    @Order(1)
    public void testRetrieveAllEmployeesLambdaIntegration() throws JsonProcessingException {
        // Convert the JSON response to a list of employees
        ObjectMapper objectMapper = new ObjectMapper();

        // Add employees
        Arrays.asList(new Employee("1", "John", "Doe", "Developer"),
                        new Employee("2", "Jane", "Smith", "Designer"))
                .forEach(employee -> {
                    dynamoDbClient.putItem(PutItemRequest.builder()
                            .tableName(DYNAMODB_TABLE_NAME)
                            .item(Map.of(
                                    "id", AttributeValue.builder().s(employee.id()).build(),
                                    "firstName", AttributeValue.builder().s(employee.firstName()).build(),
                                    "lastName", AttributeValue.builder().s(employee.lastName()).build(),
                                    "jobPosition", AttributeValue.builder().s(employee.jobPosition()).build()
                            ))
                            .build());
                });

        // Act again
        ApiGatewayResponse apiGatewayResponse = new RetrieveAllEmployeesLambda(dynamoDbClient).handleRequest(null, mockContext);

        // Assert again
        assertEquals(200, apiGatewayResponse.statusCode());

        // Convert the JSON response to a list of employees
        List<Map<String, String>> employees = objectMapper.readValue(apiGatewayResponse.body(), new TypeReference<>() {});

        // Verify that the response now contains the added employees
        assertEquals(2, employees.size());
        assertEquals("John", employees.get(0).get("firstName"));
        assertEquals("Doe", employees.get(0).get("lastName"));
        assertEquals("Developer", employees.get(0).get("jobPosition"));
        assertEquals("Jane", employees.get(1).get("firstName"));
        assertEquals("Smith", employees.get(1).get("lastName"));
        assertEquals("Designer", employees.get(1).get("jobPosition"));
    }

    @Test
    @Order(2)
    public void testRetrieveAllEmployeesLambda_ExceptionHandling() {
        // Mock
        doReturn(lambdaLogger).when(mockContext).getLogger();
        doNothing().when(lambdaLogger).log(anyString());

        // Simuler une exception en modifiant l'URL de service de DynamoDB
        DynamoDbClient faultyDynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey")))
                .endpointOverride(URI.create("http://localhost:123459")) // URL incorrecte pour simuler une exception
                .build();

        // Act
        ApiGatewayResponse apiGatewayResponse = new RetrieveAllEmployeesLambda(faultyDynamoDbClient).handleRequest(null, mockContext);

        // Assert
        assertEquals(500, apiGatewayResponse.statusCode());
        assertEquals("Error retrieving all employees.", apiGatewayResponse.body());
    }

    @Test
    @Order(3)
    public void testAddEmployeeLambda() {
        // Arrange
        Employee testEmployee = new Employee("1234", "John", "Doe", "Developer");

        // Act
        ApiGatewayResponse apiGatewayResponse = new AddEmployeeLambda(dynamoDbClient).handleRequest(testEmployee, mockContext);

        // Assert
        assertEquals("Employee added successfully with ID: 1234", apiGatewayResponse.body());

        // Check if the employee was actually added to DynamoDB
        GetItemResponse getItemResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(DYNAMODB_TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s("1234").build()))
                .build());

        assertTrue(getItemResponse.hasItem());
        assertEquals("John", getItemResponse.item().get("firstName").s());
        assertEquals("Doe", getItemResponse.item().get("lastName").s());
        assertEquals("Developer", getItemResponse.item().get("jobPosition").s());
    }

    @Test
    @Order(4)
    public void testAddEmployeeLambda_AlreadyExists() {
        // Arrange
        Employee testEmployee = new Employee("1234", "John", "Doe", "Developer");

        // Act
        ApiGatewayResponse apiGatewayResponse = new AddEmployeeLambda(dynamoDbClient).handleRequest(testEmployee, mockContext);

        // Assert
        assertEquals("Employee with ID 1234 already exists.", apiGatewayResponse.body());
        assertEquals(409, apiGatewayResponse.statusCode());
    }


    @Test
    @Order(5)
    public void testAddEmployeeLambda_ExceptionHandling() {
        // Mock
        doReturn(lambdaLogger).when(mockContext).getLogger();
        doNothing().when(lambdaLogger).log(anyString());

        // Arrange
        Employee testEmployee = new Employee("123", "John", "Doe", "Developer");

        DynamoDbClient faultyDynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey")))
                .endpointOverride(URI.create("http://localhost:12345")) // URL incorrecte pour simuler une exception
                .build();

        AddEmployeeLambda addEmployeeLambda = new AddEmployeeLambda(faultyDynamoDbClient);

        // Act
        ApiGatewayResponse apiGatewayResponse = addEmployeeLambda.handleRequest(testEmployee, mockContext);

        // Assert
        assertEquals("Error adding employee.", apiGatewayResponse.body());
        assertEquals(500, apiGatewayResponse.statusCode());
    }

    @Test
    @Order(6)
    public void testUpdateEmployeeLambda() {
        // Arrange
        Employee testEmployee = new Employee("1234", "John", "Doe", "Developer");

        // Act
        new AddEmployeeLambda(dynamoDbClient).handleRequest(testEmployee, mockContext);

        // Update the employee
        Employee updateEmployee = new Employee("1234", "UpdatedFirstName", "UpdatedLastName", "UpdatedPosition");

        ApiGatewayResponse apiGatewayResponse = new UpdateEmployeeLambda(dynamoDbClient).handleRequest(updateEmployee, mockContext);

        // Assert
        assertEquals("Employee updated successfully with ID: 1234", apiGatewayResponse.body());

        // Check if the employee was actually updated in DynamoDB
        GetItemResponse getItemResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(DYNAMODB_TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s("1234").build()))
                .build());

        assertTrue(getItemResponse.hasItem());
        assertEquals("UpdatedFirstName", getItemResponse.item().get("firstName").s());
        assertEquals("UpdatedLastName", getItemResponse.item().get("lastName").s());
        assertEquals("UpdatedPosition", getItemResponse.item().get("jobPosition").s());
    }

    @Test
    @Order(7)
    public void testUpdateEmployeeLambda_EmployeeNotFound() {
        // Arrange
        Employee updatedEmployee = new Employee("NonexistentId", "UpdatedFirstName", "UpdatedLastName", "UpdatedPosition");

        ApiGatewayResponse apiGatewayResponse = new UpdateEmployeeLambda(dynamoDbClient).handleRequest(updatedEmployee, mockContext);

        // Assert
        assertEquals(404, apiGatewayResponse.statusCode());
        assertEquals("Employee with ID NonexistentId does not exist.", apiGatewayResponse.body());
    }

    @Test
    @Order(8)
    public void testUpdateEmployeeLambda_ExceptionHandling() {

        // Mock
        doReturn(lambdaLogger).when(mockContext).getLogger();
        doNothing().when(lambdaLogger).log(anyString());

        // Arrange
        Employee updatedEmployee = new Employee("1", "UpdatedFirstName", "UpdatedLastName", "UpdatedPosition");

        // Simuler une exception en modifiant l'URL de service de DynamoDB
        DynamoDbClient faultyDynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey")))
                .endpointOverride(URI.create("http://localhost:123456")) // URL incorrecte pour simuler une exception
                .build();

        // Act
        ApiGatewayResponse apiGatewayResponse = new UpdateEmployeeLambda(faultyDynamoDbClient).handleRequest(updatedEmployee, mockContext);

        // Assert
        assertEquals(500, apiGatewayResponse.statusCode());
        assertEquals("Error updating employee.", apiGatewayResponse.body());
    }

    @Test
    @Order(9)
    public void testRetrieveEmployeeLambda() throws IOException {
        // Arrange
        Employee testEmployee = new Employee("123", "John", "Doe", "Developer");

        // Act
        new AddEmployeeLambda(dynamoDbClient).handleRequest(testEmployee, mockContext);

        // Retrieve the employee
        Map<String, String> input = new HashMap<>();
        input.put("id", testEmployee.id());

        ApiGatewayResponse apiGatewayResponse = new RetrieveEmployeeLambda(dynamoDbClient).handleRequest(input, mockContext);
        Employee resultEmployee = new ObjectMapper().readValue(apiGatewayResponse.body(), Employee.class);

        // Assert
        assertEquals(testEmployee.id(), resultEmployee.id());
        assertEquals(testEmployee.firstName(), resultEmployee.firstName());
        assertEquals(testEmployee.lastName(), resultEmployee.lastName());
        assertEquals(testEmployee.jobPosition(), resultEmployee.jobPosition());
    }

    @Test
    @Order(10)
    public void testDeleteEmployeeLambda() {
        // Arrange
        Employee testEmployee = new Employee("123", "John", "Doe", "Developer");

        // Act
        new AddEmployeeLambda(dynamoDbClient).handleRequest(testEmployee, mockContext);

        // Retrieve the employee
        Map<String, String> input = new HashMap<>();
        input.put("id", testEmployee.id());

        // Delete the employee
        ApiGatewayResponse apiGatewayResponse = new DeleteEmployeeLambda(dynamoDbClient).handleRequest(input, mockContext);

        // Assert
        assertEquals("Employee with ID 123 deleted successfully.", apiGatewayResponse.body());

        // Check if the employee was actually deleted from DynamoDB
        GetItemResponse getItemResponse = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(DYNAMODB_TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s("123").build()))
                .build());

        assertFalse(getItemResponse.hasItem());
    }

    @Test
    @Order(11)
    public void testDeleteEmployeeLambda_IdNotProvided() {
        // Arrange
        Map<String, String> input = new HashMap<>();

        // Act
        ApiGatewayResponse apiGatewayResponse = new DeleteEmployeeLambda(dynamoDbClient).handleRequest(input, mockContext);

        // Assert
        assertEquals("Invalid input. Please provide an employeeId.", apiGatewayResponse.body());
        assertEquals(400, apiGatewayResponse.statusCode());
    }

    @Test
    @Order(13)
    public void testDeleteEmployeeLambda_EmployeeNotExists() {
        // Arrange
        Map<String, String> input = new HashMap<>();
        input.put("id", "nonexistentId");

        // Act
        ApiGatewayResponse apiGatewayResponse = new DeleteEmployeeLambda(dynamoDbClient).handleRequest(input, mockContext);

        // Assert
        assertEquals("Employee with ID nonexistentId does not exist.", apiGatewayResponse.body());
        assertEquals(404, apiGatewayResponse.statusCode());
    }

    @Test
    @Order(14)
    public void testDeleteEmployeeLambda_ExceptionHandling() {
        // Mock
        doReturn(lambdaLogger).when(mockContext).getLogger();
        doNothing().when(lambdaLogger).log(anyString());

        // Arrange
        Employee testEmployee = new Employee("123", "John", "Doe", "Developer");

        // Act
        new AddEmployeeLambda(dynamoDbClient).handleRequest(testEmployee, mockContext);

        Map<String, String> input = new HashMap<>();
        input.put("id", "123");

        // Simuler une exception en modifiant l'URL de service de DynamoDB
        DynamoDbClient faultyDynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey")))
                .endpointOverride(URI.create("http://localhost:12345")) // URL incorrecte pour simuler une exception
                .build();

        ApiGatewayResponse apiGatewayResponse = new DeleteEmployeeLambda(faultyDynamoDbClient).handleRequest(input, mockContext);

        // Assert
        assertEquals("Error deleting employee.", apiGatewayResponse.body());
        assertEquals(500, apiGatewayResponse.statusCode());
    }


}
