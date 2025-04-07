Therapy Service API
Overview
The Therapy Service API is a backend service designed to manage therapy sessions, client-therapist mappings, journal entries, and messaging functionality. It is built using Java, AWS Lambda, DynamoDB, and AWS CDK for infrastructure provisioning. The APIs are designed to be RESTful and follow best practices for scalability and maintainability.

Features
Client Management: CRUD operations for managing clients.

Therapist Management: CRUD operations for managing therapists.

Session Management: Create, update, delete, and retrieve therapy sessions.

Journal Management: Manage journal entries and access permissions.

Messaging System: Send, retrieve, update, and delete messages between clients and therapists.

Mappings: Manage therapist-client mappings and journal access requests.

Tech Stack
Programming Language: Java (Runtime: Java 17)

Infrastructure as Code: AWS CDK

Database: DynamoDB (NoSQL)

API Gateway: AWS API Gateway

Serverless Functions: AWS Lambda

Testing Tool: Postman

Setup Instructions
Prerequisites
Install Java 17.

Install Maven.

Install AWS CLI and configure it:

bash
aws configure
Install AWS CDK:

bash
npm install -g aws-cdk
Ensure you have an AWS account with sufficient permissions to create resources (e.g., DynamoDB tables, Lambda functions).

Steps to Run the Project
Clone the Repository:

bash
git clone https://github.com/<your-username>/<repo-name>.git
cd <repo-name>
Install Dependencies:

Navigate to the project directory and run:

bash
mvn clean install
Build the Project:

Compile the Java code into a deployable JAR file:

bash
mvn clean package
Deploy Infrastructure Using CDK:

Bootstrap your AWS environment (only needed for first-time setup):

bash
cdk bootstrap
Deploy the stack:

bash
cdk deploy
Test APIs Using Postman:

Import the provided Postman collection (postman_collection.json) into Postman.

Use the base URL of your deployed API Gateway (e.g., https://<api-id>.execute-api.<region>.amazonaws.com/prod).

API Endpoints
Client Management
Method	Endpoint	Description
POST	/clients	Create a new client
GET	/clients	List all clients
GET	/clients/{clientId}	Get client details
PUT	/clients/{clientId}	Update client information
DELETE	/clients/{clientId}	Delete a client
Therapist Management
Method	Endpoint	Description
POST	/therapists	Create a new therapist
GET	/therapists	List all therapists
GET	/therapists/{therapistId}	Get therapist details
PUT	/therapists/{therapistId}	Update therapist information
DELETE	/therapists/{therapistId}	Delete a therapist
Session Management
Method	Endpoint	Description
POST	/sessions/{therapistId}	Create a session
GET	/sessions/{therapistId}	List all sessions for a therapist
GET	/sessions/{therapistId}/{sessionId}	Get session details
PUT	/sessions/{therapistId}/{sessionId}	Update session details
DELETE	/sessions/{therapistId}/{sessionId}	Delete a session
Database Schema
The project uses DynamoDB as the database. Below are the key tables:

1. Clients Table
Partition Key: ClientId

Attributes:

email (String)

name (String)

mappedTherapistsIds (String Set)

2. Therapists Table
Partition Key: TherapistId

Attributes:

email (String)

name (String)

expertise (String)

location (String)

3. Sessions Table
Partition Key: TherapistId

Sort Key: SessionId

Attributes:

date (String)

startTime (String)

endTime (String)

status (String)

Assumptions
All APIs are authenticated using IAM roles.

Error handling is implemented with proper HTTP status codes.

DynamoDB GSIs are used for efficient querying.
