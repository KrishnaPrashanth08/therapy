# Therapy Application

This project is a web-based application developed to manage user roles and provide a secure, role-based access system for both clients and therapists. It consists of a Node.js backend integrated with DynamoDB, with the frontend built using React. The application includes role-based authentication and access control, ensuring that users can only access pages relevant to their roles.

## Features

- **Role-Based Authentication:** 
  - Secure authentication with JWT tokens.
  - Role-based access to different pages (Client and Therapist dashboards).
  
- **Backend Integration:**
  - Node.js and Express for the backend server.
  - Integrated with AWS DynamoDB for data storage and management.
  - Environment variables are stored securely and not pushed to the repository (handled using `.env` files).

- **Frontend:**
  - React-based frontend to provide a seamless user experience.
  - Login, signup, and protected routes based on user roles.

- **Swagger API Documentation:**
  - The API is documented using Swagger (`swagger.yaml`), providing clear descriptions of available endpoints, request/response formats, and role-based restrictions.

## Setup

To run the project locally, follow the steps below.

### Prerequisites

- Node.js and npm installed on your machine.
- A DynamoDB instance set up (you can use AWS or a local version).

### Backend Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/KrishnaPrashanth08/therapy.git
   cd therapy
