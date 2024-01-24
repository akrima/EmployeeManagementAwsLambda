package com.akrima.employeemanagement;

import com.akrima.employeemanagement.model.Employee;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class AddEmployeeLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String DYNAMO_DB_TABLE_NAME = "Employee";
    private final DynamoDbClient dynamoDbClient;

    public AddEmployeeLambda() {
        // Default Constructor required aws lambda
        this.dynamoDbClient = DynamoDbClientFactory.createDynamoDbClient();
    }

    public AddEmployeeLambda(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        APIGatewayProxyResponseEvent responseEvent=new APIGatewayProxyResponseEvent();
        try {
            Employee newEmployee = new ObjectMapper().readValue(apiGatewayProxyRequestEvent.getBody(), Employee.class);
            // Check if the employee already exists
            if (employeeExists(newEmployee.id())) {
                return responseEvent.withStatusCode(409).withBody("Employee with ID " + newEmployee.id() + " already exists.");
            }

            // If the employee doesn't exist, add them to DynamoDB
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(DYNAMO_DB_TABLE_NAME)
                    .item(employeeToDynamoDbItem(newEmployee))
                    .build());

            return responseEvent.withStatusCode(201).withBody("Employee added successfully with ID: " + newEmployee.id());
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error adding employee: " + e.getMessage());
            return responseEvent.withStatusCode(500).withBody("Error adding employee.");
        }
    }

    private java.util.Map<String, AttributeValue> employeeToDynamoDbItem(Employee employee) {
        return Map.of(
                "id", AttributeValue.builder().s(employee.id()).build(),
                "firstName", AttributeValue.builder().s(employee.firstName()).build(),
                "lastName", AttributeValue.builder().s(employee.lastName()).build(),
                "jobPosition", AttributeValue.builder().s(employee.jobPosition()).build()
        );
    }

    public boolean employeeExists(String id) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(DYNAMO_DB_TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s(id).build()))
                .build());
        return response.hasItem();
    }
}
