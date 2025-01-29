import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.List;

/**
 * Azure Functions with HTTP Trigger.
 */
public class LabOneAPI {
    private static final Key JWT_SECRET = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * Hello World endpoint that returns a simple greeting
     */
    @FunctionName("hello")
    public HttpResponseMessage hello(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Void> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Java HTTP trigger function processed a request.");

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "text/plain")
                .body("Hello World")
                .build();
    }

    /**
     * Login endpoint that validates email and generates JWT token
     */
    @FunctionName("login")
    public HttpResponseMessage login(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<LoginRequest> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing login request");
        
        LoginRequest loginRequest = request.getBody();
        
        if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please provide username and password")
                    .build();
        }

        if (!EMAIL_PATTERN.matcher(loginRequest.getUsername()).matches()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid email format")
                    .build();
        }

        String token = Jwts.builder()
                .setSubject(loginRequest.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(JWT_SECRET)
                .compact();

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"token\": \"" + token + "\"}")
                .build();
    }

    /**
     * Order processing endpoint that reads messages from Service Bus Queue
     */
    @FunctionName("process-order")
    public void processOrder(
            @ServiceBusQueueTrigger(
                name = "message",
                queueName = "lab1queue",
                connection = "ServiceBusConnection")
                String message,
            final ExecutionContext context) {

        try {
            context.getLogger().info("Starting Service Bus trigger processing");
            context.getLogger().info("Processing message from Service Bus: " + message);
            
            // Log the message processing
            String processMessage = String.format("Order processing started for message: %s", message);
            context.getLogger().info(processMessage);
            
        } catch (Exception e) {
            context.getLogger().severe("Error in processOrder: " + e.getMessage());
            context.getLogger().severe("Stack trace: " + e.getStackTrace().toString());
            throw e; // Rethrow to ensure Azure knows about the failure
        }
    }

    /**
     * Daily sales report endpoint that generates random sales data
     */
    @FunctionName("daily-sales-report")
    public HttpResponseMessage dailySalesReport(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Void> request,
            final ExecutionContext context) {

        Random random = new Random();
        int salesCount = random.nextInt(100) + 1; // Generate number between 1-100

        String report = String.format(
            "Daily Sales Report\n" +
            "Date: %s\n" +
            "Total Sales: %d\n" +
            "Performance: %s",
            java.time.LocalDate.now(),
            salesCount,
            getSalesPerformanceMessage(salesCount)
        );

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "text/plain")
                .body(report)
                .build();
    }

    private String getSalesPerformanceMessage(int sales) {
        if (sales > 75) return "Excellent day!";
        if (sales > 50) return "Good day!";
        if (sales > 25) return "Average day.";
        return "Below average day.";
    }

    /**
     * Model class for login request data
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Model class for order request data
     */
    public static class OrderRequest {
        private String orderId;
        private List<String> items;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }
    }
} 