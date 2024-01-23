package com.akrima.employeemanagement;

import com.akrima.employeemanagement.model.Employee;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class AddEmployeeLambda implements RequestHandler<Employee, ApiGatewayResponse> {

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
    public ApiGatewayResponse handleRequest(Employee newEmployee, Context context) {
        try {
            // Check if the employee already exists
            if (employeeExists(newEmployee.id())) {
                return new ApiGatewayResponse("Employee with ID " + newEmployee.id() + " already exists.", 409);
            }

            // If the employee doesn't exist, add them to DynamoDB
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(DYNAMO_DB_TABLE_NAME)
                    .item(employeeToDynamoDbItem(newEmployee))
                    .build());

            return new ApiGatewayResponse("Employee added successfully with ID: " + newEmployee.id(), 201);
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error adding employee: " + e.getMessage());
            return new ApiGatewayResponse("Error adding employee.", 500);
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
