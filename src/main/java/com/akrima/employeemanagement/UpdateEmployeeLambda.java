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

public class UpdateEmployeeLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        APIGatewayProxyResponseEvent responseEvent=new APIGatewayProxyResponseEvent();
        try {
            Employee updatedEmployee = new ObjectMapper().readValue(apiGatewayProxyRequestEvent.getBody(), Employee.class);
            // Check if the employee exists
            if (!employeeExists(updatedEmployee.id())) {
                return responseEvent.withStatusCode(404).withBody("Employee with ID " + updatedEmployee.id() + " does not exist.");
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

            return responseEvent.withStatusCode(200).withBody("Employee updated successfully with ID: " + updatedEmployee.id());
        } catch (Exception e) {
            // Handle any errors
            context.getLogger().log("Error updating employee: " + e.getMessage());
            return responseEvent.withStatusCode(500).withBody("Error updating employee.");
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
