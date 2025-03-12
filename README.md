Therapy Management System
Overview
The Therapy Management System is a comprehensive platform designed to streamline interactions between therapists and clients. It provides a robust backend API built with Express.js and integrates with AWS DynamoDB for scalable and efficient data storage.

Key Features
User Authentication: Secure signup and login functionality for both clients and therapists.

Client Management: CRUD operations for managing client profiles.

Therapist Management: CRUD operations for managing therapist profiles.

Appointment Scheduling: Therapists can create available slots, and clients can request appointments.

Journal System:

Clients can create journal entries with emotional tracking.

Therapists can add session notes (private or shared).

Messaging System: Secure communication channel between clients and therapists.

Access Control: Therapists can request access to client journals, which clients can approve or reject.

Tech Stack
Backend: Node.js with Express.js

Database: AWS DynamoDB

Authentication: JSON Web Tokens (JWT) and bcrypt for secure user sessions

API Documentation: Swagger UI

Getting Started
Prerequisites
Before starting the project, ensure you have the following installed:

Node.js

AWS CLI (configured with your credentials)

DynamoDB tables set up as per the schema

Clone the Repository
bash
git clone https://github.com/KrishnaPrashanth08/therapy.git
cd therapy
Install Dependencies
bash
npm install
Set Up Environment Variables
Create a .env file in the root directory with the following variables:

text
PORT=5000
JWT_SECRET=your_jwt_secret_key
AWS_REGION=your_aws_region
DYNAMODB_ENDPOINT=your_dynamodb_endpoint (optional, if using local DynamoDB)
Set Up DynamoDB Tables
Ensure you have created the required DynamoDB tables based on the schema provided in the documentation.

Start the Server
bash
npm start
The server will run on http://localhost:5000.

API Documentation
The API documentation is available at http://localhost:5000/api-docs using Swagger UI. It provides detailed information about all endpoints, including request/response formats.

Database Schema Overview
Clients Table

Primary Key: ClientId

Attributes: email, name

Therapists Table

Primary Key: TherapistId

Attributes: email, expertise, location, mappedClientsIds, name

SessionSlots Table

Primary Key: SlotId

Attributes: date, startTime, endTime, status, therapistId

AppointmentRequests Table

Primary Key: AppointmentRequestId

Attributes: ClientId, SlotId, status, TherapistId

Sessions Table

Primary Key: SessionId

Attributes: clientId, therapistId, date, startTime, endTime, privateNotes, sharedNotes, status

JournalEntries Table

Primary Key: JournalEntryId

Attributes: clientId, content, createdAt, feeling, intensity, time

JournalAccessRequests Table

Primary Key: JournalAccessRequestId

Attributes: ClientId, TherapistId, createdAt, status

Messages Table

Primary Key: messageId

Attributes: content, recipientId, senderId, status, timestamp

MappingRequests Table

Primary Key: mappingRequestId

Attributes: clientId, therapistId, createdAt, status

MappedTherapists Table

Primary Key: Partition Key (clientId)

Sort Key (therapistId)

Attributes:  mappedAt

Key Endpoints
Authentication
POST /signup: Register a new user (client or therapist).

POST /login: Authenticate a user and return a JWT token.

Client Management
GET /clients: Retrieve all clients.

POST /clients: Create a new client profile.

PUT /clients/{clientId}: Update a client profile.

DELETE /clients/{clientId}: Delete a client profile.

Therapist Management
GET /therapists: Retrieve all therapists.

POST /therapists: Create a new therapist profile.

PUT /therapists/{therapistId}: Update a therapist profile.

DELETE /therapists/{therapistId}: Delete a therapist profile.

Appointment Scheduling
POST /session-slots/{therapistId}: Create session slots for a therapist.

POST /appointment-requests: Request an appointment for a session slot.

Journal System
POST /journals/{clientId}: Add a journal entry for a client.

GET /journals/{clientId}: Retrieve all journal entries for a client.

Messaging System
POST /messages: Send a message between client and therapist.

GET /messages: Retrieve message history between client and therapist.

Security Features
Passwords are securely hashed using bcrypt before storage in the database.

JWT tokens are used to authenticate API requests securely.

CORS is enabled to allow secure cross-origin API calls.

Future Enhancements
Implement real-time notifications for messages and appointment requests using WebSockets or AWS SNS.

Add an analytics dashboard for therapists to track session data and trends.

Integrate video calling functionality for remote therapy sessions.
