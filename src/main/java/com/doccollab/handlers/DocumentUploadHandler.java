package com.doccollab.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.doccollab.utils.ResponseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DocumentUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final S3Client s3Client = S3Client.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String documentsTable = System.getenv("DOCUMENTS_TABLE");
    private final String historyTable = System.getenv("DOCUMENT_HISTORY_TABLE");
    private final String s3Bucket = System.getenv("S3_BUCKET");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            // Parse request body
            JsonNode requestBody = objectMapper.readTree(request.getBody());
            String fileName = requestBody.get("fileName").asText();
            String fileContent = requestBody.get("fileContent").asText();

            // Generate unique document ID
            String documentId = "doc-" + UUID.randomUUID().toString().substring(0, 8);
            String timestamp = Instant.now().toString();

            // Save original file to S3 uploads folder
            String uploadKey = "uploads/" + fileName;
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(uploadKey)
                            .build(),
                    RequestBody.fromString(fileContent)
            );

            // Save first version to S3 documents folder
            String documentKey = "documents/" + documentId + "-v1.txt";
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(documentKey)
                            .build(),
                    RequestBody.fromString(fileContent)
            );

            // Create document record in DynamoDB
            Map<String, AttributeValue> documentItem = new HashMap<>();
            documentItem.put("documentId", AttributeValue.builder().s(documentId).build());
            documentItem.put("title", AttributeValue.builder().s(fileName).build());
            documentItem.put("originalFile", AttributeValue.builder().s(uploadKey).build());
            documentItem.put("currentFile", AttributeValue.builder().s(documentKey).build());
            documentItem.put("currentEditNumber", AttributeValue.builder().n("1").build());
            documentItem.put("createdAt", AttributeValue.builder().s(timestamp).build());
            documentItem.put("lastModified", AttributeValue.builder().s(timestamp).build());

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(documentsTable)
                    .item(documentItem)
                    .build());

            // Create first history record
            Map<String, AttributeValue> historyItem = new HashMap<>();
            historyItem.put("documentId", AttributeValue.builder().s(documentId).build());
            historyItem.put("editNumber", AttributeValue.builder().n("1").build());
            historyItem.put("filePath", AttributeValue.builder().s(documentKey).build());
            historyItem.put("editedAt", AttributeValue.builder().s(timestamp).build());
            historyItem.put("editedBy", AttributeValue.builder().s("uploader").build());
            historyItem.put("changeDescription", AttributeValue.builder().s("Initial upload").build());

            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(historyTable)
                    .item(historyItem)
                    .build());

            // Create success response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("documentId", documentId);
            responseBody.put("message", "Document uploaded successfully");
            responseBody.put("editNumber", 1);

            return ResponseUtil.createResponse(200, responseBody);

        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return ResponseUtil.createErrorResponse(500, e.getMessage());
        }
    }
}
