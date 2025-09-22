# 📄 Cloud-Based Document Collaboration Platform

A serverless document collaboration platform built on AWS, enabling real-time document sharing, editing, and version control similar to Google Docs.

## 🚀 Features

- **📤 Document Upload**: Upload documents with automatic ID generation
- **📖 Real-time Retrieval**: Get current document content instantly
- **✏️ Version Control**: Edit documents with automatic version tracking (v1, v2, v3...)
- **📚 Edit History**: Complete audit trail of all document changes
- **🔒 Secure Storage**: Enterprise-grade security with AWS IAM
- **💰 Cost-Effective**: Pay-per-use serverless architecture
- **⚡ High Performance**: Sub-second response times
- **🌐 Scalable**: Handles 50+ concurrent users automatically

## 🏗️ Architecture

### Technology Stack
- **Backend**: Java 11 on AWS Lambda
- **Database**: Amazon DynamoDB (NoSQL)
- **Storage**: Amazon S3 with versioning
- **API**: Amazon API Gateway (REST)
- **Security**: AWS IAM roles and policies
- **Build Tool**: Maven

### System Architecture


Frontend → API Gateway → Lambda Functions → DynamoDB + S3
↓
CloudWatch (Monitoring)



## 📊 Performance Metrics

- **Uptime**: 99%+ availability
- **Response Time**: <500ms for 95% of requests
- **Concurrent Users**: 50+ supported
- **Storage**: Unlimited document storage
- **Versions**: Unlimited version history per document

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/upload` | Upload new document |
| GET | `/documents/{id}` | Get current document |
| PUT | `/documents/{id}` | Edit document (creates new version) |
| GET | `/documents/{id}/history` | Get all edit versions |

### Example Usage

**Upload Document:**
```bash
curl -X POST https://api-url/upload \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "my-document.txt",
    "fileContent": "Hello World!"
  }'

Response :
JSON :- {
  "success": true,
  "documentId": "doc-abc12345",
  "message": "Document uploaded successfully",
  "editNumber": 1
}



💰 Cost Analysis
Monthly costs for 50 concurrent users:

Lambda: $15-25
DynamoDB: $20-40
S3: $10-20
API Gateway: $5-15
Total: $50-100/month
🛠️ Quick Start
Prerequisites
AWS Account with administrative access
Java 11+
Maven 3.6+
AWS CLI configured


1. Clone Repository:-

git clone https://github.com/Taahier/CollabDocs.git
cd CollabDocs

2. Build Application :-

mvn clean package

3. Deploy to AWS :-

Create DynamoDB tables (Documents, DocumentHistory)
Create S3 bucket with folders (uploads/, documents/)
Create Lambda functions and upload JAR
Configure API Gateway endpoints
Set up IAM roles and permissions
4. Test the System
Update API URL in frontend and test all endpoints.

🗂️ Data Flow :-

Upload: User uploads document → Stored in S3 → Metadata in DynamoDB
Edit: User edits document → New version created → History recorded
Retrieve: User requests document → Fetched from S3 + DynamoDB
History: User requests history → All versions listed


💾 Database Schema :- 
Documents Table :
JSON :

{
  "documentId": "doc-abc12345",
  "title": "my-document.txt",
  "currentEditNumber": 3,
  "createdAt": "2025-01-15T10:00:00Z",
  "lastModified": "2025-01-15T10:30:00Z"
}


DocumentHistory Table :-
JSON :

{
  "documentId": "doc-abc12345",
  "editNumber": 3,
  "editedAt": "2025-01-15T10:30:00Z",
  "editedBy": "user-john",
  "changeDescription": "Updated conclusion"
}



👨‍💻 Author
Taahier Mhd

GitHub: @Taahier
LinkedIn: Taahier
📄 License
This project is licensed under the MIT License.
