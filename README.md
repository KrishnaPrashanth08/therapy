Therapy Management System
Overview
This project is a comprehensive Therapy Management System designed to facilitate interactions between therapists and clients. It provides a robust backend API built with Express.js and integrates with AWS DynamoDB for data storage.
Features
User Authentication: Secure signup and login functionality for both clients and therapists.
Client Management: CRUD operations for client profiles.
Therapist Management: CRUD operations for therapist profiles.
Appointment Scheduling: Therapists can create available slots, and clients can request appointments.
Journal System:
Clients can create journal entries with emotional tracking.
Therapists can add session notes (private or shared).
Messaging System: Secure communication channel between clients and therapists.
Access Control: Therapists can request access to client journals, which clients can approve or reject.
Tech Stack
Backend: Node.js with Express.js
Database: AWS DynamoDB
Authentication
API Documentation: Swagger UI
Getting Started
Clone the repository
Install dependencies: npm install
Set up your AWS credentials and DynamoDB table
Start the server: npm start
API Documentation
API documentation is available at the /api-docs endpoint using Swagger UI.
Key Endpoints
/signup and /login: User authentication
/clients and /therapists: User management
/sessions: Session management
/journals: Journal entry management
/messages: Messaging system
Security
Passwords are hashed using bcrypt before storage
JWT is used for maintaining user sessions
CORS is enabled for secure cross-origin requests
Future Enhancements
Implement real-time notifications for messages and appointment requests
Add analytics dashboard for therapists
Integrate video calling feature for remote sessions
Contributing
Contributions are welcome! Please feel free to submit a Pull Request.
License
This project is licensed under the MIT License.
