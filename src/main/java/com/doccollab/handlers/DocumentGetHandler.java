package com.doccollab.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.doccollab.utils.ResponseUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;

import java.util.HashMap;
import java.util.Map;

public class DocumentGetHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final S3Client s3Client = S3Client.create();

    private final String documentsTable = System.getenv("DOCUMENTS_TABLE");
    private final String s3Bucket = System.getenv("S3_BUCKET");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            // Get documentId from path parameters
            String documentId = request.getPathParameters().get("documentId");

            if (documentId == null || documentId.isEmpty()) {
                return ResponseUtil.createErrorResponse(400, "Document ID is required");
            }

            // Get document metadata from DynamoDB
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(documentsTable)
                    .key(Map.of("documentId", AttributeValue.builder().s(documentId).build()))
                    .build());

            if (!response.hasItem()) {
                return ResponseUtil.createErrorResponse(404, "Document not found");
            }

            Map<String, AttributeValue> item = response.item();

            // Get current file path from DynamoDB
            String currentFile = item.get("currentFile").s();

            // Get file content from S3
            ResponseInputStream<?> s3Object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(currentFile)
                    .build());

            String content = new String(s3Object.readAllBytes());

            // Build response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("documentId", documentId);
            responseBody.put("title", item.get("title").s());
            responseBody.put("content", content);
            responseBody.put("currentEditNumber", Integer.parseInt(item.get("currentEditNumber").n()));
            responseBody.put("createdAt", item.get("createdAt").s());
            responseBody.put("lastModified", item.get("lastModified").s());

            return ResponseUtil.createResponse(200, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ResponseUtil.createErrorResponse(500, e.getMessage());
        }
    }
}