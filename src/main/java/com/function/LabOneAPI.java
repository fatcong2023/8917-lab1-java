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

import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class LabOneAPI {
    private static final Key JWT_SECRET = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

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
            String processMessage = String.format("Order processing started for message: %s", message);
            context.getLogger().info(processMessage);
        } catch (Exception e) {
            context.getLogger().severe("Error in processOrder: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Daily sales report endpoint that generates random sales data
     * Runs every day at midnight (0 0 0 * * *)
     */
    @FunctionName("daily-sales-report")
    public void dailySalesReport(
            @TimerTrigger(
                name = "dailySalesTrigger",
                schedule = "0 0 0 * * *")    // Runs at midnight every day
                String timerInfo,
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

        // Log the report instead of returning it
        context.getLogger().info(report);
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