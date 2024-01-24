package com.akrima.employeemanagement;

import com.akrima.employeemanagement.model.Employee;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RetrieveAllEmployeesLambda implements RequestHandler<Void, APIGatewayProxyResponseEvent> {

    private static final String DYNAMO_DB_TABLE_NAME = "Employee";
    private final DynamoDbClient dynamoDbClient;

    public RetrieveAllEmployeesLambda() {
        // Default Constructor required aws lambda
        this.dynamoDbClient = DynamoDbClientFactory.createDynamoDbClient();
    }
    public RetrieveAllEmployeesLambda(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(Void input, Context context) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        try {
            List<Employee> allEmployees = getAllEmployeesFromDynamoDb();

            // Convert the list of employees to JSON
            String jsonResponse = new ObjectMapper().writeValueAsString(allEmployees);

            return responseEvent.withStatusCode(200).withBody(jsonResponse);
        } catch (Exception e) {
            context.getLogger().log("Error retrieving all employees: " + e.getMessage());
            return responseEvent.withStatusCode(500).withBody("Error retrieving all employees.");
        }
    }

    private List<Employee> getAllEmployeesFromDynamoDb() {
        ScanRequest scanRequest = ScanRequest.builder().tableName(DYNAMO_DB_TABLE_NAME).build();
        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        return scanResponse.items().stream()
                .map(this::mapDynamoDbItemToEmployee)
                .collect(Collectors.toList());
    }

    private Employee mapDynamoDbItemToEmployee(Map<String, AttributeValue> item) {
        return new Employee(
                item.get("id").s(),
                item.get("firstName").s(),
                item.get("lastName").s(),
                item.get("jobPosition").s()
        );
    }
}
