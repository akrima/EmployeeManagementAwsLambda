package com.akrima.employeemanagement;

import com.akrima.employeemanagement.model.Employee;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

public class RetrieveEmployeeLambda implements RequestHandler<APIGatewayProxyRequestEvent,  APIGatewayProxyResponseEvent> {

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
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        try {

            // Check if the id is in the url path
            Map<String, String> pathParameters = apiGatewayProxyRequestEvent.getPathParameters();
            if (pathParameters == null || pathParameters.size() != 1) {
                return responseEvent.withStatusCode(400).withBody("Invalid input. Please provide an employeeId.");
            }
            String employeeId = pathParameters.get("id");
            // Check if the employee exists
            if (!employeeExists(employeeId)) {
                return responseEvent.withStatusCode(404).withBody("Employee with ID " + employeeId + " does not exist.");
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
            return responseEvent.withStatusCode(200).withBody(new ObjectMapper().writeValueAsString(retrievedEmployee));
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error retrieving employee: " + e.getMessage());
            return responseEvent.withStatusCode(505).withBody("Error retrieving employee.");
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
