package com.akrima.employeemanagement;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbClientFactory {

    public static DynamoDbClient createDynamoDbClient() {
        return DynamoDbClient.create();
    }
}
