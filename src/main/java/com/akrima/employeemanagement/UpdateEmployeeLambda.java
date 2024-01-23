package com.akrima.employeemanagement;

import com.akrima.employeemanagement.model.Employee;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class UpdateEmployeeLambda implements RequestHandler<Employee, ApiGatewayResponse> {

    private static final String DYNAMO_DB_TABLE_NAME = "Employee";
    private final DynamoDbClient dynamoDbClient;

    public UpdateEmployeeLambda() {
        // Default Constructor required aws lambda
        this.dynamoDbClient = DynamoDbClientFactory.createDynamoDbClient();
    }
    public UpdateEmployeeLambda(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public ApiGatewayResponse handleRequest(Employee updatedEmployee, Context context) {
        try {
            // Check if the employee exists
            if (!employeeExists(updatedEmployee.id())) {
                return new ApiGatewayResponse("Employee with ID " + updatedEmployee.id() + " does not exist.", 404);
            }

            // If the employee exists, update their information in DynamoDB
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(DYNAMO_DB_TABLE_NAME)
                    .key(Map.of("id", AttributeValue.builder().s(updatedEmployee.id()).build()))
                    .updateExpression("SET firstName = :firstName, lastName = :lastName, jobPosition = :newJobPosition")
                    .expressionAttributeValues(Map.of(
                            ":firstName", AttributeValue.builder().s(updatedEmployee.firstName()).build(),
                            ":lastName", AttributeValue.builder().s(updatedEmployee.lastName()).build(),
                            ":newJobPosition", AttributeValue.builder().s(updatedEmployee.jobPosition()).build()
                    ))
                    .build());

            return new ApiGatewayResponse("Employee updated successfully with ID: " + updatedEmployee.id(), 200);
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error updating employee: " + e.getMessage());
            return new ApiGatewayResponse("Error updating employee.", 500);
        }
    }

    private boolean employeeExists(String id) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(DYNAMO_DB_TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s(id).build()))
                .build());
        return response.hasItem();
    }
}
