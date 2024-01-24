package com.akrima.employeemanagement;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

public class DeleteEmployeeLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        try {
            // Check if the id is in the url path
            Map<String, String> pathParameters = apiGatewayProxyRequestEvent.getPathParameters();
            if (pathParameters == null || pathParameters.size() != 1) {
                return responseEvent.withStatusCode(400).withBody("Invalid input. Please provide an employeeId.");
            }
            String employeeId = pathParameters.get("id");
            if (!employeeExists(employeeId)) {
                return responseEvent.withStatusCode(404).withBody("Employee with ID " + employeeId + " does not exist.");
            }

            // If the employee exists, delete them from DynamoDB
            DeleteItemResponse response = dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                    .tableName(DYNAMO_DB_TABLE_NAME)
                    .key(Map.of("id", AttributeValue.builder().s(employeeId).build()))
                    .build());

            return responseEvent.withStatusCode(202).withBody("Employee with ID " + employeeId + " deleted successfully.");
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error deleting employee: " + e.getMessage());
            return responseEvent.withStatusCode(500).withBody("Error deleting employee.");
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
