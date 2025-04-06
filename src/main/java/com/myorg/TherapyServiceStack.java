package com.myorg;

import software.constructs.Construct;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Duration;


import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;

import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Code;

import java.util.List;
import java.util.Map;

public class TherapyServiceStack extends Stack {
    public TherapyServiceStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public TherapyServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Define DynamoDB Clients Table
        Table clientsTable = Table.Builder.create(this, "ClientsTable")
            .partitionKey(Attribute.builder()
                .name("ClientId") // Partition Key
                .type(AttributeType.STRING)
                .build())
            .billingMode(BillingMode.PAY_PER_REQUEST) // On-demand billing
            .build();

			        // Add Global Secondary Index: EmailIndex
			        clientsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			            .indexName("EmailIndex")
			            .partitionKey(Attribute.builder()
			                .name("email") // Partition Key for GSI
			                .type(AttributeType.STRING)
			                .build())
			            .projectionType(ProjectionType.ALL) // Include all attributes in the index
			            .build());
			
			        // Add Global Secondary Index: MappedTherapistsIdIndex
			        clientsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			            .indexName("MappedTherapistsIdIndex")
			            .partitionKey(Attribute.builder()
			                .name("mappedTherapistsIds") // Partition Key for GSI
			                .type(AttributeType.STRING)
			                .build())
			            .projectionType(ProjectionType.ALL) // Include all attributes in the index
			            .build());
        
		// Define Therapists Table with GSIs
        Table therapistsTable = Table.Builder.create(this, "TherapistsTable")
            .partitionKey(Attribute.builder()
                .name("TherapistId")
                .type(AttributeType.STRING)
                .build())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build();
        			
			     // EmailIndex GSI
			        therapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			            .indexName("EmailIndex")
			            .partitionKey(Attribute.builder()
			                .name("email")
			                .type(AttributeType.STRING)
			                .build())
			            .projectionType(ProjectionType.ALL)
			            .build());
			        // Location Index
			        therapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			            .indexName("locationIndex")
			            .partitionKey(Attribute.builder()
			                .name("location")
			                .type(AttributeType.STRING)
			                .build())
			            .projectionType(ProjectionType.ALL)
			            .build());
			        
			        // Expertise Index
			        therapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			            .indexName("ExpertiseIndex") // Case-sensitive match to your schema
			            .partitionKey(Attribute.builder()
			                .name("expertise")
			                .type(AttributeType.STRING)
			                .build())
			            .projectionType(ProjectionType.ALL)
			            .build());
			
			       
			   
			      
			 // Define Session Slots Table
			  Table sessionSlotsTable = Table.Builder.create(this, "SessionSlotsTable")
			        	    .partitionKey(Attribute.builder().name("TherapistId").type(AttributeType.STRING).build())
			        	    .sortKey(Attribute.builder().name("SlotId").type(AttributeType.STRING).build())
			        	    .billingMode(BillingMode.PAY_PER_REQUEST)
			        	    .build();
			  
			  
			// Create AppointmentRequests Table
		        Table appointmentRequestsTable = Table.Builder.create(this, "AppointmentRequestsTable")
		            .partitionKey(Attribute.builder()
		                .name("TherapistId")
		                .type(AttributeType.STRING)
		                .build())
		            .sortKey(Attribute.builder()
		                .name("AppointmentRequestId")
		                .type(AttributeType.STRING)
		                .build())
		            .billingMode(BillingMode.PAY_PER_REQUEST)
		            .build();

						        // Add ClientAppointmentsIndex GSI
						        appointmentRequestsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
						            .indexName("ClientAppointmentsIndex")
						            .partitionKey(Attribute.builder()
						                .name("ClientId")
						                .type(AttributeType.STRING)
						                .build())
						            .sortKey(Attribute.builder()
						                .name("status")
						                .type(AttributeType.STRING)
						                .build())
						            .projectionType(ProjectionType.ALL)
						            .build());
						        
	        Table sessionsTable = Table.Builder.create(this, "SessionsTable")
	                .partitionKey(Attribute.builder().name("TherapistId").type(AttributeType.STRING).build())
	                .sortKey(Attribute.builder().name("SessionId").type(AttributeType.STRING).build())
	                .billingMode(BillingMode.PAY_PER_REQUEST)
	                .build();

	            // Add ClientSessionsIndex GSI
	            sessionsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
	                .indexName("ClientSessionsIndex")
	                .partitionKey(Attribute.builder().name("clientId").type(AttributeType.STRING).build())
	                .sortKey(Attribute.builder().name("date").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build());
	            
	            
	         // ====================== JOURNAL ENTRIES TABLE ======================
	            Table journalEntriesTable = Table.Builder.create(this, "JournalEntriesTable")
	                .partitionKey(Attribute.builder().name("ClientId").type(AttributeType.STRING).build())
	                .sortKey(Attribute.builder().name("JournalEntryId").type(AttributeType.STRING).build())
	                .billingMode(BillingMode.PAY_PER_REQUEST)
	                .build();

			            // Add DateIndex GSI (ClientId as partition key, Date as sort key)
			            journalEntriesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			                .indexName("DateIndex")
			                .partitionKey(Attribute.builder().name("ClientId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("Date").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build());
			            
			            
	         // ====================== JOURNAL ACCESS REQUESTS TABLE ======================
	            Table journalAccessRequestsTable = Table.Builder.create(this, "JournalAccessRequestsTable")
	                .partitionKey(Attribute.builder().name("ClientId").type(AttributeType.STRING).build())
	                .sortKey(Attribute.builder().name("JournalAccessRequestId").type(AttributeType.STRING).build())
	                .billingMode(BillingMode.PAY_PER_REQUEST)
	                .build();

			            // Add TherapistsRequestIndex GSI
			            journalAccessRequestsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			                .indexName("TherapistsRequestIndex")
			                .partitionKey(Attribute.builder().name("TherapistId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("createdAt").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build()); 
			            
			         // ====================== MAPPED THERAPISTS TABLE ======================
			            Table mappedTherapistsTable = Table.Builder.create(this, "MappedTherapists")
			                .partitionKey(Attribute.builder().name("ClientId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("TherapistId").type(AttributeType.STRING).build())
			                .billingMode(BillingMode.PAY_PER_REQUEST)
			                .build();

			            // Add Global Secondary Index
			            mappedTherapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			                .indexName("TherapistClientIndex")
			                .partitionKey(Attribute.builder().name("TherapistId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("mappedAt").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build());
			            
			         // ====================== MAPPING REQUESTS TABLE ======================
			            Table mappingRequestsTable = Table.Builder.create(this, "MappingRequestsTable")
			                .partitionKey(Attribute.builder().name("ClientId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("mappingRequestId").type(AttributeType.STRING).build())
			                .billingMode(BillingMode.PAY_PER_REQUEST)
			                .build();

			            // Add Global Secondary Index
			            mappingRequestsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			                .indexName("TherapistRequestsIndex")
			                .partitionKey(Attribute.builder().name("therapistId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("createdAt").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build()); 
			            
			         // ====================== MESSAGES TABLE ======================
			            Table messagesTable = Table.Builder.create(this, "MessagesTable")
			                .partitionKey(Attribute.builder().name("messageId").type(AttributeType.STRING).build())
			                .billingMode(BillingMode.PAY_PER_REQUEST)
			                .build();

			            // Add Global Secondary Indexes
			            messagesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			                .indexName("SenderIndex")
			                .partitionKey(Attribute.builder().name("senderId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("timestamp").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build());

			            messagesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			                .indexName("RecipientIndex")
			                .partitionKey(Attribute.builder().name("recipientId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("timestamp").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build());

			            messagesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
			                .indexName("ConversationIndex")
			                .partitionKey(Attribute.builder().name("senderId").type(AttributeType.STRING).build())
			                .sortKey(Attribute.builder().name("recipientId").type(AttributeType.STRING).build())
			                .projectionType(ProjectionType.ALL)
			                .build());
			            
			            
   

        // Define Lambda Function for POST /clients (Create Client)
        Function createClientLambda = Function.Builder.create(this, "CreateClientLambda")
            .runtime(Runtime.JAVA_17) // Use Java 17 runtime
            .handler("com.example.handlers.clients.CreateClientHandler::handleRequest") // Fully qualified handler name
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))// Path to compiled JAR file
            .environment(Map.of(
                "CLIENTS_TABLE", clientsTable.getTableName() // Pass table name as environment variable
            ))
            .build();
        //lambda function for listing all clients
        Function listClientsLambda = Function.Builder.create(this, "ListClientsLambda")
                .runtime(Runtime.JAVA_17) // Use Java 17 runtime
                .handler("com.example.handlers.clients.ListClientsHandler::handleRequest") // Fully qualified handler name
                .code(Code.fromAsset("target/therapy-service-0.1.jar")) // Path to compiled JAR file
                .timeout(Duration.seconds(30)) // Timeout configuration
                .environment(Map.of(
                        "CLIENTS_TABLE", clientsTable.getTableName() // Pass table name as environment variable
                ))
                .build();
        // Define Lambda for GET /clients/{clientId}
        Function getClientLambda = Function.Builder.create(this, "GetClientLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.clients.GetClientHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of("CLIENTS_TABLE", clientsTable.getTableName()))
            .build();
     // Define Lambda for PUT /clients/{clientId}
        Function updateClientLambda = Function.Builder.create(this, "UpdateClientLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.clients.UpdateClientHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of("CLIENTS_TABLE", clientsTable.getTableName()))
            .build();
     // Define Lambda Function for DELETE /clients/{clientId}
        Function deleteClientLambda = Function.Builder.create(this, "DeleteClientLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.clients.DeleteClientHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of("CLIENTS_TABLE", clientsTable.getTableName()))
            .build();
        
     // Create Therapist Lambda
        Function createTherapistLambda = Function.Builder.create(this, "CreateTherapistLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.therapists.CreateTherapistHandler")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of("THERAPISTS_TABLE", therapistsTable.getTableName()))
            .build();
        therapistsTable.grantWriteData(createTherapistLambda);
        
     // List Therapists Lambda
        Function listTherapistsLambda = Function.Builder.create(this, "ListTherapistsLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.therapists.ListTherapistsHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of("THERAPISTS_TABLE", therapistsTable.getTableName()))
            .build();
        therapistsTable.grantReadData(listTherapistsLambda);
        
     // Get Therapist by ID Lambda
        Function getTherapistLambda = Function.Builder.create(this, "GetTherapistLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.therapists.GetTherapistHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of("THERAPISTS_TABLE", therapistsTable.getTableName()))
            .build();
        therapistsTable.grantReadData(getTherapistLambda);
        
     // Update Therapist Lambda
        Function updateTherapistLambda = Function.Builder.create(this, "UpdateTherapistLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.therapists.UpdateTherapistHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of(
                "THERAPISTS_TABLE", therapistsTable.getTableName()
            ))
            .build();
        therapistsTable.grantReadWriteData(updateTherapistLambda);
        
     // Delete Therapist Lambda
        Function deleteTherapistLambda = Function.Builder.create(this, "DeleteTherapistLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.therapists.DeleteTherapistHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of("THERAPISTS_TABLE", therapistsTable.getTableName()))
            .build();

        therapistsTable.grantWriteData(deleteTherapistLambda);
        
        

        clientsTable.grantReadWriteData(updateClientLambda);
        

        clientsTable.grantReadData(getClientLambda);

        // Grant permissions to Lambda to access DynamoDB
        clientsTable.grantReadWriteData(createClientLambda);
        // Grant permissions to Lambda to access DynamoDB
        clientsTable.grantReadData(listClientsLambda);
        // Grant permissions to Lambda to access DynamoDB
        clientsTable.grantWriteData(deleteClientLambda);
        
        
     // Create Session Slot Lambda
        Function createSessionSlotLambda = Function.Builder.create(this, "CreateSessionSlotLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.sessionslots.CreateSessionSlotHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of(
                "SESSION_SLOTS_TABLE", sessionSlotsTable.getTableName()
            ))
            .build();

        sessionSlotsTable.grantWriteData(createSessionSlotLambda);
        
     // List Session Slots Lambda
        Function listSessionSlotsLambda = Function.Builder.create(this, "ListSessionSlotsLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.sessionslots.ListSessionSlotsHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of(
                "SESSION_SLOTS_TABLE", sessionSlotsTable.getTableName()
            ))
            .build();

        sessionSlotsTable.grantReadData(listSessionSlotsLambda);
        
        // Request Appointment Lambda
        Function requestAppointmentLambda = Function.Builder.create(this, "RequestAppointmentLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.appointments.RequestAppointmentHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of(
                "SESSION_SLOTS_TABLE", sessionSlotsTable.getTableName(),
                "APPOINTMENT_REQUESTS_TABLE", appointmentRequestsTable.getTableName()
            ))
            .timeout(Duration.seconds(30))
            .build();
        
	        sessionSlotsTable.grantReadData(requestAppointmentLambda);
	        appointmentRequestsTable.grantWriteData(requestAppointmentLambda);
        
        
     // List Appointments Lambda
        Function listAppointmentsLambda = Function.Builder.create(this, "ListAppointmentsLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.appointments.ListAppointmentsHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of(
            		"APPOINTMENT_REQUESTS_TABLE", appointmentRequestsTable.getTableName()
            ))
            .build();

        appointmentRequestsTable.grantReadData(listAppointmentsLambda);
        listAppointmentsLambda.addToRolePolicy(PolicyStatement.Builder.create()
            .actions(List.of("dynamodb:Query"))
            .resources(List.of(appointmentRequestsTable.getTableArn()))
            .build());
        
        //approve or reject appintment 
        //approveAppointment endpoint put
        Function approveAppointmentLambda = Function.Builder.create(this, "ApproveAppointmentLambda")
                .runtime(Runtime.JAVA_17)
                .handler("com.example.handlers.sessions.ApproveAppointmentHandler::handleRequest")
                .code(Code.fromAsset("target/therapy-service-0.1.jar"))
                .timeout(Duration.seconds(30))
                .environment(Map.of(
                    "APPOINTMENT_REQUESTS_TABLE", appointmentRequestsTable.getTableName(),
                    "SESSIONS_TABLE", sessionsTable.getTableName()
                ))
                .build();
        
        // Grant permissions
        appointmentRequestsTable.grantReadWriteData(approveAppointmentLambda);
        sessionsTable.grantWriteData(approveAppointmentLambda);
        
     // Create Get Session Lambda
        Function getSessionLambda = Function.Builder.create(this, "GetSessionLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.sessions.GetSessionHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .environment(Map.of("SESSIONS_TABLE", sessionsTable.getTableName()))
            .timeout(Duration.seconds(30))
            .build();
        
        sessionsTable.grantReadData(getSessionLambda);
        
     // Update Session Lambda
        Function updateSessionLambda = Function.Builder.create(this, "UpdateSessionLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.sessions.UpdateSessionHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .environment(Map.of("SESSIONS_TABLE", sessionsTable.getTableName()))
            .timeout(Duration.seconds(30))
            .build();

        sessionsTable.grantReadWriteData(updateSessionLambda);
        
     // Define Delete Session Lambda
        Function deleteSessionLambda = Function.Builder.create(this, "DeleteSessionLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.sessions.DeleteSessionHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .environment(Map.of("SESSIONS_TABLE", sessionsTable.getTableName()))
            .timeout(Duration.seconds(30))
            .build();
        
        sessionsTable.grantWriteData(deleteSessionLambda);
        
     // ====================== JOURNAL LAMBDA ======================
        Function createJournalEntryLambda = Function.Builder.create(this, "CreateJournalEntryLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.journals.CreateJournalEntryHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of(
                "JOURNAL_ENTRIES_TABLE", journalEntriesTable.getTableName()
            ))
            .timeout(Duration.seconds(30))
            .build();

        journalEntriesTable.grantWriteData(createJournalEntryLambda);
        
     // Create Get Journal Entry Lambda
        Function listJournalEntriesLambda = Function.Builder.create(this, "ListJournalEntriesLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.journals.ListJournalEntriesHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .timeout(Duration.seconds(30))
            .environment(Map.of(
                "JOURNAL_ENTRIES_TABLE", journalEntriesTable.getTableName()
            ))
            .build();

        journalEntriesTable.grantReadData(listJournalEntriesLambda);
        
     // Get Single Journal Entry Lambda
        Function getJournalEntryLambda = Function.Builder.create(this, "GetJournalEntryLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.journals.GetJournalEntryHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .environment(Map.of(
                "JOURNAL_ENTRIES_TABLE", journalEntriesTable.getTableName()
            ))
            .timeout(Duration.seconds(30))
            .build();

        journalEntriesTable.grantReadData(getJournalEntryLambda);
        
     // Define Update Journal Entry Lambda
        Function updateJournalEntryLambda = Function.Builder.create(this, "UpdateJournalEntryLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.example.handlers.journals.UpdateJournalEntryHandler::handleRequest")
            .code(Code.fromAsset("target/therapy-service-0.1.jar"))
            .environment(Map.of(
                "JOURNAL_ENTRIES_TABLE", journalEntriesTable.getTableName()
            ))
            .timeout(Duration.seconds(30))
            .build();

        journalEntriesTable.grantReadWriteData(updateJournalEntryLambda);

        Function deleteJournalEntryLambda = Function.Builder.create(this, "DeleteJournalEntryLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.journals.DeleteJournalEntryHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of("JOURNAL_ENTRIES_TABLE", journalEntriesTable.getTableName()))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	journalEntriesTable.grantWriteData(deleteJournalEntryLambda);
        
        	// ====================== REQUEST ACCESS LAMBDA ======================
        	Function requestJournalAccessLambda = Function.Builder.create(this, "RequestJournalAccessLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.journals.RequestJournalAccessHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "JOURNAL_ACCESS_REQUESTS_TABLE", journalAccessRequestsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	journalAccessRequestsTable.grantWriteData(requestJournalAccessLambda);
        
        	// ====================== LIST ACCESS REQUESTS LAMBDA ======================
        	Function listJournalAccessRequestsLambda = Function.Builder.create(this, "ListJournalAccessRequestsLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.journals.ListJournalAccessRequestsHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "JOURNAL_ACCESS_REQUESTS_TABLE", journalAccessRequestsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	journalAccessRequestsTable.grantReadData(listJournalAccessRequestsLambda);
        	
        	Function approveJournalAccessLambda = Function.Builder.create(this, "ApproveJournalAccessLambda")
        		    .runtime(Runtime.JAVA_17)
        		    .handler("com.example.handlers.journals.ApproveJournalAccessHandler::handleRequest")
        		    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        		    .environment(Map.of(
        		        "JOURNAL_ACCESS_REQUESTS_TABLE", journalAccessRequestsTable.getTableName(),
        		        "MAPPED_THERAPISTS_TABLE", mappedTherapistsTable.getTableName()
        		    ))
        		    .timeout(Duration.seconds(30))
        		    .build();

        		journalAccessRequestsTable.grantReadWriteData(approveJournalAccessLambda);
        		mappedTherapistsTable.grantWriteData(approveJournalAccessLambda);
        	
        	// ====================== UPDATE PERMISSIONS LAMBDA ======================
        	Function updateJournalPermissionsLambda = Function.Builder.create(this, "UpdateJournalPermissionsLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.journals.UpdateJournalPermissionsHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "MAPPED_THERAPISTS_TABLE", mappedTherapistsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	mappedTherapistsTable.grantReadWriteData(updateJournalPermissionsLambda);
        	
        	
        	// ====================== MESSAGE LAMBDAS ======================
        	Function createMessageLambda = Function.Builder.create(this, "CreateMessageLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.messages.CreateMessageHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	Function listMessagesLambda = Function.Builder.create(this, "ListMessagesLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.messages.ListMessagesHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	Function getMessageLambda = Function.Builder.create(this, "GetMessageLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.messages.GetMessageHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	Function updateMessageLambda = Function.Builder.create(this, "UpdateMessageLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.messages.UpdateMessageHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	Function deleteMessageLambda = Function.Builder.create(this, "DeleteMessageLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.messages.DeleteMessageHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of("MESSAGES_TABLE", messagesTable.getTableName()))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	// Grant permissions
        	messagesTable.grantReadWriteData(createMessageLambda);
        	messagesTable.grantReadData(listMessagesLambda);
        	messagesTable.grantReadData(getMessageLambda);
        	messagesTable.grantReadWriteData(updateMessageLambda);
        	messagesTable.grantWriteData(deleteMessageLambda);
        	
        	
        	
        	// ====================== mapping LAMBDA FUNCTIONS ======================
        	Function requestMappingLambda = Function.Builder.create(this, "RequestMappingLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.mapping.RequestMappingHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "MAPPING_REQUESTS_TABLE", mappingRequestsTable.getTableName(),
        	        "MAPPED_THERAPISTS_TABLE", mappedTherapistsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	Function approveRejectMappingLambda = Function.Builder.create(this, "ApproveRejectMappingLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.mapping.ApproveRejectMappingHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "MAPPING_REQUESTS_TABLE", mappingRequestsTable.getTableName(),
        	        "MAPPED_THERAPISTS_TABLE", mappedTherapistsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	Function removeMappingLambda = Function.Builder.create(this, "RemoveMappingLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.mapping.RemoveMappingHandler::handleRequest")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "MAPPED_THERAPISTS_TABLE", mappedTherapistsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	// Grant permissions
        	mappingRequestsTable.grantReadWriteData(requestMappingLambda);
        	mappingRequestsTable.grantReadWriteData(approveRejectMappingLambda);
        	mappedTherapistsTable.grantReadWriteData(requestMappingLambda);
        	mappedTherapistsTable.grantReadWriteData(approveRejectMappingLambda);
        	mappedTherapistsTable.grantWriteData(removeMappingLambda);
        	
        	// Search Lambdas
        	Function clientSearchLambda = Function.Builder.create(this, "ClientSearchLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.search.ClientSearchHandler")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "CLIENTS_TABLE", clientsTable.getTableName(),
        	        "THERAPISTS_TABLE", therapistsTable.getTableName(),
        	        "JOURNAL_ENTRIES_TABLE", journalEntriesTable.getTableName(),
        	        "SESSIONS_TABLE", sessionsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();

        	Function therapistSearchLambda = Function.Builder.create(this, "TherapistSearchLambda")
        	    .runtime(Runtime.JAVA_17)
        	    .handler("com.example.handlers.search.TherapistSearchHandler")
        	    .code(Code.fromAsset("target/therapy-service-0.1.jar"))
        	    .environment(Map.of(
        	        "CLIENTS_TABLE", clientsTable.getTableName(),
        	        "JOURNAL_ENTRIES_TABLE", journalEntriesTable.getTableName(),
        	        "SESSIONS_TABLE", sessionsTable.getTableName(),
        	        "MAPPED_THERAPISTS_TABLE", mappedTherapistsTable.getTableName()
        	    ))
        	    .timeout(Duration.seconds(30))
        	    .build();
        	
        	  // Client Search Lambda Permissions
        	clientSearchLambda.addToRolePolicy(PolicyStatement.Builder.create()
        		    .actions(List.of("dynamodb:Query"))
        		    .resources(List.of(
        		        therapistsTable.getTableArn() + "/index/locationIndex", // New
        		        therapistsTable.getTableArn() + "/index/ExpertiseIndex", // New
        		        journalEntriesTable.getTableArn(),
        		        sessionsTable.getTableArn() + "/index/ClientSessionsIndex"
        		    ))
        		    .build());

            // Therapist Search Lambda Permissions
            therapistSearchLambda.addToRolePolicy(PolicyStatement.Builder.create()
                .actions(List.of("dynamodb:Query"))
                .resources(List.of(
                    clientsTable.getTableArn(),
                    journalEntriesTable.getTableArn(),
                    sessionsTable.getTableArn(),
                    mappedTherapistsTable.getTableArn() + "/index/TherapistClientIndex"
                ))
                .build());

        	// Grant permissions
        	clientsTable.grantReadData(clientSearchLambda);
        	therapistsTable.grantReadData(clientSearchLambda);
        	journalEntriesTable.grantReadData(clientSearchLambda);
        	sessionsTable.grantReadData(clientSearchLambda);

        	clientsTable.grantReadData(therapistSearchLambda);
        	journalEntriesTable.grantReadData(therapistSearchLambda);
        	sessionsTable.grantReadData(therapistSearchLambda);
        	mappedTherapistsTable.grantReadData(therapistSearchLambda);
        	
        	
        	
        	
        	// Define API Gateway
        	RestApi api = RestApi.Builder.create(this, "TherapyServiceApi")
        	    .restApiName("Therapy Service API")
        	    .description("API for managing therapy sessions.")
        	    .build();

        	// ====================== ROOT RESOURCES ======================
        	// Define all root resources FIRST
        	Resource clients = api.getRoot().addResource("clients");
        	Resource therapists = api.getRoot().addResource("therapists");
        	Resource journal = api.getRoot().addResource("journal");
        	Resource sessions = api.getRoot().addResource("sessions");
        	Resource messages = api.getRoot().addResource("messages");

        	// ====================== CLIENT MANAGEMENT ======================
        	// Client root operations
        	clients.addMethod("POST", new LambdaIntegration(createClientLambda));
        	clients.addMethod("GET", new LambdaIntegration(listClientsLambda));

        	// Client-specific endpoints
        	Resource client = clients.addResource("{clientId}");
        	client.addMethod("GET", new LambdaIntegration(getClientLambda));
        	client.addMethod("PUT", new LambdaIntegration(updateClientLambda));
        	client.addMethod("DELETE", new LambdaIntegration(deleteClientLambda));

        	// ====================== MAPPING ENDPOINTS ======================
        	Resource clientMappingRequests = client.addResource("mapping-requests");
        	Resource specificMappingRequest = clientMappingRequests.addResource("{therapistId}");
        	specificMappingRequest.addMethod("PUT", new LambdaIntegration(approveRejectMappingLambda));

        	Resource mappedTherapists = client.addResource("mapped-therapists");
        	Resource specificMappedTherapist = mappedTherapists.addResource("{therapistId}");
        	specificMappedTherapist.addMethod("DELETE", new LambdaIntegration(removeMappingLambda));

        	// ====================== THERAPIST MANAGEMENT ======================
        	Resource therapist = therapists.addResource("{therapistId}");
        	Resource mappingRequests = therapist.addResource("mapping-requests");
        	mappingRequests.addMethod("POST", new LambdaIntegration(requestMappingLambda));

        	// Session Slots (under therapist)
        	Resource sessionSlots = therapist.addResource("session-slots");
        	sessionSlots.addMethod("POST", new LambdaIntegration(createSessionSlotLambda));
        	sessionSlots.addMethod("GET", new LambdaIntegration(listSessionSlotsLambda));

        	// Appointments (under session slots)
        	Resource appointments = sessionSlots.addResource("appointments");
        	appointments.addMethod("POST", new LambdaIntegration(requestAppointmentLambda));
        	appointments.addMethod("GET", new LambdaIntegration(listAppointmentsLambda));

        	// ====================== JOURNAL ENDPOINTS ======================
        	Resource journalClients = journal.addResource("clients");
        	Resource journalClient = journalClients.addResource("{clientId}");

        	// Journal entries
        	Resource entries = journalClient.addResource("entries");
        	entries.addMethod("POST", new LambdaIntegration(createJournalEntryLambda));
        	entries.addMethod("GET", new LambdaIntegration(listJournalEntriesLambda));

        	Resource journalEntry = journalClient.addResource("{journalEntryId}");
        	journalEntry.addMethod("GET", new LambdaIntegration(getJournalEntryLambda));
        	journalEntry.addMethod("PUT", new LambdaIntegration(updateJournalEntryLambda));
        	journalEntry.addMethod("DELETE", new LambdaIntegration(deleteJournalEntryLambda));

        	// Journal permissions
        	Resource permissions = journalClient.addResource("journal-access-permissions");
        	permissions.addMethod("PUT", new LambdaIntegration(updateJournalPermissionsLambda));

        	Resource accessRequests = journalClient.addResource("access-requests");
        	accessRequests.addMethod("GET", new LambdaIntegration(listJournalAccessRequestsLambda));

        	Resource approve = journalClient.addResource("approve");
        	approve.addMethod("POST", new LambdaIntegration(approveJournalAccessLambda));

        	// Therapist journal access
        	Resource journalTherapists = journal.addResource("therapists");
        	Resource therapistJournal = journalTherapists.addResource("{therapistId}");
        	Resource requestAccess = therapistJournal.addResource("request-access");
        	requestAccess.addMethod("POST", new LambdaIntegration(requestJournalAccessLambda));

        	// ====================== SESSION MANAGEMENT ======================
        	Resource therapistSessions = sessions.addResource("{therapistId}");
        	Resource specificSession = therapistSessions.addResource("{sessionId}");
        	specificSession.addMethod("GET", new LambdaIntegration(getSessionLambda));
        	specificSession.addMethod("PUT", new LambdaIntegration(updateSessionLambda));
        	specificSession.addMethod("DELETE", new LambdaIntegration(deleteSessionLambda));

        	// ====================== MESSAGES ======================
        	messages.addMethod("POST", new LambdaIntegration(createMessageLambda));
        	messages.addMethod("GET", new LambdaIntegration(listMessagesLambda));

        	Resource message = messages.addResource("{messageId}");
        	message.addMethod("GET", new LambdaIntegration(getMessageLambda));
        	message.addMethod("PUT", new LambdaIntegration(updateMessageLambda));
        	message.addMethod("DELETE", new LambdaIntegration(deleteMessageLambda));

        	// ====================== SEARCH ENDPOINTS ======================
        	Resource clientSearch = client.addResource("search");
        	clientSearch.addMethod("GET", new LambdaIntegration(clientSearchLambda));

        	Resource therapistSearch = therapist.addResource("search");
        	therapistSearch.addMethod("GET", new LambdaIntegration(therapistSearchLambda));
    }
}
