package com.akrima.employeemanagement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class DeleteEmployeeLambda implements RequestHandler<Map<String, String>, ApiGatewayResponse> {

    private static final String DYNAMO_DB_TABLE_NAME = "Employee";
    private final DynamoDbClient dynamoDbClient;

    public DeleteEmployeeLambda() {
        // Default Constructor required aws lambda
        this.dynamoDbClient = DynamoDbClientFactory.createDynamoDbClient();
    }
    public DeleteEmployeeLambda(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public ApiGatewayResponse handleRequest(Map<String, String> input, Context context) {
        try {
            // Check if the employee exists
            if (!input.containsKey("id")) {
                return new ApiGatewayResponse("Invalid input. Please provide an employeeId.", 400);
            }

            String employeeId = input.get("id");

            if (!employeeExists(employeeId)) {
                return new ApiGatewayResponse("Employee with ID " + employeeId + " does not exist.", 404);
            }

            // If the employee exists, delete them from DynamoDB
            DeleteItemResponse response = dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(DYNAMO_DB_TABLE_NAME)
                    .key(Map.of("id", AttributeValue.builder().s(employeeId).build()))
                    .build());

            return new ApiGatewayResponse("Employee with ID " + employeeId + " deleted successfully.", 200);
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error deleting employee: " + e.getMessage());
            return new ApiGatewayResponse("Error deleting employee.", 500);
        }
    }

    private boolean employeeExists(String employeeId) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(DYNAMO_DB_TABLE_NAME)
                .key(Map.of("id", AttributeValue.builder().s(employeeId).build()))
                .build());
        return response.hasItem();
    }
}
