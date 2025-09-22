

##  1: docs/ARCHITECTURE.md

```markdown
# 🏗️ System Architecture

## Overview
The Document Collaboration Platform follows a serverless, event-driven architecture built entirely on AWS managed services.

## Architecture Diagram
```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                             │
├─────────────────┬─────────────────┬─────────────────────────────┤
│   Web Browser   │  Mobile Client  │    Desktop Client           │
└─────────────────┴─────────────────┴─────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  • REST API Endpoints                                          │
│  • Request/Response Transformation                             │
│  • Rate Limiting & Throttling                                  │
│  • CORS Configuration                                          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Lambda Function Layer                       │
├─────────────────┬─────────────────┬─────────────────────────────┤
│  Upload Handler │   Get Handler   │    Edit Handler             │
│                 │                 │                             │
│  • File Upload  │  • Document     │  • Version Control         │
│  • ID Generation│    Retrieval    │  • History Tracking        │
│  • Initial Ver. │  • Content      │  • Metadata Update         │
│                 │    Formatting   │                             │
└─────────────────┴─────────────────┴─────────────────────────────┘
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   DynamoDB      │ │       S3        │ │   CloudWatch    │
│   (Metadata)    │ │  (File Storage) │ │  (Monitoring)   │
├─────────────────┤ ├─────────────────┤ ├─────────────────┤
│ • Documents     │ │ • uploads/      │ │ • Function Logs │
│ • History       │ │ • documents/    │ │ • Metrics       │
│ • Sessions      │ │ • Versioning    │ │ • Alarms        │
│ • Permissions   │ │ • Encryption    │ │ • Dashboards    │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

## Component Details

### 1. API Gateway
- **Type**: REST API
- **Endpoints**: 4 main endpoints
- **Features**: Lambda Proxy Integration, CORS enabled, Request validation

### 2. Lambda Functions

#### DocumentUploadHandler
- **Runtime**: Java 11
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Responsibilities**: Generate unique document ID, Store files in S3, Create DynamoDB records

#### DocumentGetHandler
- **Runtime**: Java 11
- **Memory**: 256 MB
- **Timeout**: 15 seconds
- **Responsibilities**: Retrieve document metadata, Fetch file content, Format response

#### DocumentEditHandler
- **Runtime**: Java 11
- **Memory**: 512 MB
- **Timeout**: 30 seconds
- **Responsibilities**: Increment version number, Store new version, Update metadata, Create history record

#### DocumentHistoryHandler
- **Runtime**: Java 11
- **Memory**: 256 MB
- **Timeout**: 15 seconds
- **Responsibilities**: Query all versions, Format history response, Return chronological list

### 3. DynamoDB Tables

#### Documents Table
- **Partition Key**: documentId (String)
- **Capacity**: On-demand
- **Purpose**: Store current document metadata

#### DocumentHistory Table
- **Partition Key**: documentId (String)
- **Sort Key**: editNumber (Number)
- **Capacity**: On-demand
- **Purpose**: Store all edit versions

### 4. S3 Storage

#### Bucket Structure
```
document-collaboration-storage/
├── uploads/
│   ├── original-file-1.txt
│   ├── original-file-2.docx
│   └── original-file-3.pdf
└── documents/
    ├── doc-abc123-v1.txt
    ├── doc-abc123-v2.txt
    ├── doc-abc123-v3.txt
    ├── doc-def456-v1.docx
    └── doc-def456-v2.docx
```

## Data Flow Patterns

### 1. Document Upload Flow
```
User → API Gateway → UploadHandler → S3 (uploads/) → S3 (documents/) → DynamoDB → Response
```

### 2. Document Retrieval Flow
```
User → API Gateway → GetHandler → DynamoDB (metadata) → S3 (content) → Response
```

### 3. Document Edit Flow
```
User → API Gateway → EditHandler → DynamoDB (get current) → S3 (new version) → DynamoDB (update) → Response
```

### 4. History Retrieval Flow
```
User → API Gateway → HistoryHandler → DynamoDB (query all versions) → Response
```

## Security Architecture

### 1. Identity & Access Management
- **Lambda Execution Role**: Least privilege access
- **Resource-based Policies**: Fine-grained permissions
- **API Gateway**: Optional API keys and usage plans

### 2. Data Security
- **S3 Encryption**: SSE-S3 server-side encryption
- **DynamoDB**: Encryption at rest
- **API Gateway**: HTTPS only

### 3. Network Security
- **VPC**: Optional VPC deployment
- **Security Groups**: Controlled access
- **CORS**: Configured for web access
```

##  2: docs/API_DOCUMENTATION.md

```markdown
# 📡 API Documentation

## Base URL
```
https://your-api-id.execute-api.region.amazonaws.com/prod
```

## Authentication
Currently, the API uses AWS IAM for backend authentication. No API keys required for testing.

## Endpoints

### 1. Upload Document

**Endpoint**: `POST /upload`

**Description**: Upload a new document and create the first version.

**Request Body**:
```json
{
  "fileName": "my-document.txt",
  "fileContent": "Hello World! This is my document content."
}
```

**Response**:
```json
{
  "success": true,
  "documentId": "doc-abc12345",
  "message": "Document uploaded successfully",
  "editNumber": 1
}
```

**Example**:
```bash
curl -X POST https://api-url/upload \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "test.txt",
    "fileContent": "Hello World!"
  }'
```

### 2. Get Document

**Endpoint**: `GET /documents/{documentId}`

**Description**: Retrieve the current version of a document.

**Path Parameters**:
- `documentId` (string): Unique document identifier

**Response**:
```json
{
  "success": true,
  "documentId": "doc-abc12345",
  "title": "my-document.txt",
  "content": "Hello World! This is my document content.",
  "currentEditNumber": 1,
  "createdAt": "2025-01-15T10:00:00Z",
  "lastModified": "2025-01-15T10:00:00Z"
}
```

**Example**:
```bash
curl https://api-url/documents/doc-abc12345
```

### 3. Edit Document

**Endpoint**: `PUT /documents/{documentId}`

**Description**: Edit a document and create a new version.

**Path Parameters**:
- `documentId` (string): Unique document identifier

**Request Body**:
```json
{
  "content": "Updated content for my document!",
  "editedBy": "john-doe",
  "changeDescription": "Fixed typos and added conclusion"
}
```

**Response**:
```json
{
  "success": true,
  "documentId": "doc-abc12345",
  "message": "Document updated successfully",
  "editNumber": 2,
  "editedBy": "john-doe",
  "editedAt": "2025-01-15T10:30:00Z"
}
```

**Example**:
```bash
curl -X PUT https://api-url/documents/doc-abc12345 \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Updated content",
    "editedBy": "user123",
    "changeDescription": "Minor updates"
  }'
```

### 4. Get Document History

**Endpoint**: `GET /documents/{documentId}/history`

**Description**: Get all edit versions of a document.

**Path Parameters**:
- `documentId` (string): Unique document identifier

**Response**:
```json
{
  "success": true,
  "documentId": "doc-abc12345",
  "totalEdits": 2,
  "history": [
    {
      "editNumber": 1,
      "editedAt": "2025-01-15T10:00:00Z",
      "editedBy": "uploader",
      "changeDescription": "Initial upload",
      "filePath": "documents/doc-abc12345-v1.txt"
    },
    {
      "editNumber": 2,
      "editedAt": "2025-01-15T10:30:00Z",
      "editedBy": "john-doe",
      "changeDescription": "Fixed typos and added conclusion",
      "filePath": "documents/doc-abc12345-v2.txt"
    }
  ]
}
```

**Example**:
```bash
curl https://api-url/documents/doc-abc12345/history
```

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "error": "Document ID is required"
}
```

### 404 Not Found
```json
{
  "success": false,
  "error": "Document not found"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "error": "Internal server error message"
}
```

## Rate Limits
- **Default**: 1000 requests per second
- **Burst**: 2000 requests
- **Daily**: 10,000 requests

## Response Headers
All responses include CORS headers:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

## Data Formats

### Document ID Format
- Pattern: `doc-[8-character-uuid]`
- Example: `doc-abc12345`

### Timestamp Format
- Format: ISO 8601 UTC
- Example: `2025-01-15T10:30:00Z`

### File Naming Convention
- Uploads: `uploads/original-filename.ext`
- Versions: `documents/doc-id-v{number}.txt`
```

## File 3: docs/DEPLOYMENT_GUIDE.md

```markdown
# 🚀 Deployment Guide

## Prerequisites

### Required Tools
- AWS Account with administrative access
- Java 11 or higher
- Maven 3.6+
- AWS CLI configured

### AWS Services Used
- AWS Lambda
- Amazon DynamoDB
- Amazon S3
- Amazon API Gateway
- AWS IAM

## Step-by-Step Deployment

### 1. Setup AWS CLI
```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter your default region (e.g., us-east-1)
# Enter output format: json
```

### 2. Create IAM Role

1. **Go to IAM Console → Roles → Create Role**
2. **Select**: AWS Service → Lambda
3. **Attach policies**:
   - `AWSLambdaBasicExecutionRole`
   - `AmazonDynamoDBFullAccess`
   - `AmazonS3FullAccess`
   - `AmazonSNSFullAccess`
4. **Role name**: `DocumentCollaborationLambdaRole`
5. **Create role**

### 3. Create DynamoDB Tables

#### Documents Table
```bash
aws dynamodb create-table \
    --table-name Documents \
    --attribute-definitions \
        AttributeName=documentId,AttributeType=S \
    --key-schema \
        AttributeName=documentId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST
```

#### DocumentHistory Table
```bash
aws dynamodb create-table \
    --table-name DocumentHistory \
    --attribute-definitions \
        AttributeName=documentId,AttributeType=S \
        AttributeName=editNumber,AttributeType=N \
    --key-schema \
        AttributeName=documentId,KeyType=HASH \
        AttributeName=editNumber,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST
```

### 4. Create S3 Bucket

```bash
# Replace 'your-unique-bucket-name' with your actual bucket name
aws s3 mb s3://document-collaboration-storage-your-initials

# Enable versioning
aws s3api put-bucket-versioning \
    --bucket document-collaboration-storage-your-initials \
    --versioning-configuration Status=Enabled

# Create folder structure
aws s3api put-object \
    --bucket document-collaboration-storage-your-initials \
    --key uploads/
    
aws s3api put-object \
    --bucket document-collaboration-storage-your-initials \
    --key documents/
```

### 5. Build and Deploy Lambda Functions

#### Build the application
```bash
mvn clean package
```

#### Create Lambda functions
```bash
# Upload function
aws lambda create-function \
    --function-name DocumentUploadFunction \
    --runtime java11 \
    --role arn:aws:iam::YOUR-ACCOUNT-ID:role/DocumentCollaborationLambdaRole \
    --handler com.doccollab.handlers.DocumentUploadHandler::handleRequest \
    --zip-file fileb://target/document-collaboration-lambda.jar \
    --timeout 30 \
    --memory-size 512

# Get function
aws lambda create-function \
    --function-name DocumentGetFunction \
    --runtime java11 \
    --role arn:aws:iam::YOUR-ACCOUNT-ID:role/DocumentCollaborationLambdaRole \
    --handler com.doccollab.handlers.DocumentGetHandler::handleRequest \
    --zip-file fileb://target/document-collaboration-lambda.jar \
    --timeout 15 \
    --memory-size 256

# Edit function
aws lambda create-function \
    --function-name DocumentEditFunction \
    --runtime java11 \
    --role arn:aws:iam::YOUR-ACCOUNT-ID:role/DocumentCollaborationLambdaRole \
    --handler com.doccollab.handlers.DocumentEditHandler::handleRequest \
    --zip-file fileb://target/document-collaboration-lambda.jar \
    --timeout 30 \
    --memory-size 512

# History function
aws lambda create-function \
    --function-name DocumentHistoryFunction \
    --runtime java11 \
    --role arn:aws:iam::YOUR-ACCOUNT-ID:role/DocumentCollaborationLambdaRole \
    --handler com.doccollab.handlers.DocumentHistoryHandler::handleRequest \
    --zip-file fileb://target/document-collaboration-lambda.jar \
    --timeout 15 \
    --memory-size 256
```

#### Set environment variables for each function
```bash
aws lambda update-function-configuration \
    --function-
