package com.akrima.employeemanagement;

import com.akrima.employeemanagement.model.Employee;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

public class RetrieveEmployeeLambda implements RequestHandler<Map<String, String>, ApiGatewayResponse> {

    private static final String DYNAMO_DB_TABLE_NAME = "Employee";
    private final DynamoDbClient dynamoDbClient;

    public RetrieveEmployeeLambda() {
        // Default Constructor required aws lambda
        this.dynamoDbClient = DynamoDbClientFactory.createDynamoDbClient();
    }
    public RetrieveEmployeeLambda(DynamoDbClient dynamoDbClient) {
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

            // If the employee exists, retrieve their information from DynamoDB
            Map<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("id", AttributeValue.builder().s(employeeId).build());

            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(DYNAMO_DB_TABLE_NAME)
                    .key(keyMap)
                    .build());

            Map<String, AttributeValue> item = response.item();

            // Map DynamoDB item to Employee object
            Employee retrievedEmployee = new Employee(
                    item.get("id").s(),
                    item.get("firstName").s(),
                    item.get("lastName").s(),
                    item.get("jobPosition").s()
            );
            return new ApiGatewayResponse(new ObjectMapper().writeValueAsString(retrievedEmployee), 200);
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error retrieving employee: " + e.getMessage());
            return new ApiGatewayResponse("Error retrieving employee.", 500);
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
