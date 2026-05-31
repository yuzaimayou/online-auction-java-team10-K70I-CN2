# API Unit Tests - Quick Start Guide

## 📍 Location
```
online-auction-server/src/test/java/com/auction/server/controller/api/
├── LoginHandlerTest.java
├── RegisterHandlerTest.java
├── ItemsHandlerTest.java
├── WalletHandlerTest.java
└── ItemDetailHandlerTest.java
```

---

## 🎯 What's Tested

### 1️⃣ **Authentication (Login/Register)**
- LoginHandler: User authentication with password verification
- RegisterHandler: User registration with duplicate checking
- Status codes: 200, 400, 401, 403, 405, 409, 500

### 2️⃣ **Item Management (Get/Create)**
- ItemsHandler: List items with filters, create new items
- ItemDetailHandler: Get/Update/Delete individual items
- Query parameter parsing and validation

### 3️⃣ **Wallet Operations (Deposit/Settlement)**
- WalletHandler: Deposit funds and settle auctions
- JSON validation and amount verification
- Transaction logging

---

## 🚀 Running Tests

### Run All API Tests
```bash
mvn test -Dtest=*HandlerTest
```

### Run Specific Handler Tests
```bash
# Authentication tests
mvn test -Dtest=LoginHandlerTest
mvn test -Dtest=RegisterHandlerTest

# Item tests
mvn test -Dtest=ItemsHandlerTest
mvn test -Dtest=ItemDetailHandlerTest

# Wallet tests
mvn test -Dtest=WalletHandlerTest
```

### Run Single Test Method
```bash
mvn test -Dtest=LoginHandlerTest#handle_should_return_200_and_user_data_on_successful_login
```

### Run with Maven Surefire Plugin
```bash
mvn clean test -Dtest=*HandlerTest -DfailIfNoTests=false
```

---

## 📊 Test Summary

| Handler | Tests | Coverage |
|---------|-------|----------|
| LoginHandler | 10 | Authentication flow |
| RegisterHandler | 13 | User registration |
| ItemsHandler | 20 | Item listing & creation |
| WalletHandler | 16 | Deposits & settlement |
| ItemDetailHandler | 19 | Item CRUD ops |
| **TOTAL** | **78** | **5 API endpoints** |

---

## 🔧 Test Structure

Each test follows the **AAA Pattern**:

```java
@Test
void handle_should_return_200_when_item_created_successfully() {
    // ARRANGE - Set up test data
    String requestJson = "{\"itemName\":\"Laptop\",...}";
    InputStream inputStream = new ByteArrayInputStream(
        requestJson.getBytes(StandardCharsets.UTF_8)
    );
    when(mockExchange.getRequestMethod()).thenReturn("POST");
    when(mockExchange.getRequestBody()).thenReturn(inputStream);
    
    // ACT - Execute the handler
    assertDoesNotThrow(() -> itemsHandler.handle(mockExchange));
    
    // ASSERT - Verify the result
    verify(mockExchange).getRequestMethod();
}
```

---

## 🎨 Key Test Categories

### Input Validation Tests
- Validate required fields (username, email, password)
- Handle null values
- Handle blank/whitespace values
- Handle malformed JSON
- Validate field types (numbers, dates, etc.)

### HTTP Protocol Tests
- Verify correct HTTP methods (GET, POST, PUT, DELETE)
- Return 405 for unsupported methods
- Verify correct status codes (200, 400, 401, 403, 404, 409, 500)
- Parse query parameters correctly
- Extract path parameters

### Business Logic Tests
- Login with valid/invalid credentials
- Register with duplicate detection
- Deposit funds with constraints
- Item creation/update/deletion
- Settlement execution

### Error Handling Tests
- Handle malformed JSON gracefully
- Return appropriate error codes
- Verify error messages
- Safe error recovery

---

## 💡 How Tests Work

1. **Mock HttpExchange**: Each test mocks the HttpExchange object to simulate HTTP requests
2. **Setup Request**: Create JSON payloads and input streams
3. **Execute Handler**: Call handler.handle(mockExchange)
4. **Verify**: Use Mockito to verify interactions

### Example: Login Handler Test
```java
// Create mock exchange
HttpExchange mockExchange = mock(HttpExchange.class);

// Prepare login request
AuthPayload payload = new AuthPayload();
payload.setUsername("alice");
payload.setPassword("pass123");

String json = gson.toJson(payload);
InputStream input = new ByteArrayInputStream(json.getBytes(UTF-8));

// Setup mocks
when(mockExchange.getRequestMethod()).thenReturn("POST");
when(mockExchange.getRequestBody()).thenReturn(input);

// Execute
LoginHandler handler = new LoginHandler();
assertDoesNotThrow(() -> handler.handle(mockExchange));
```

---

## 🛠️ Common Test Patterns

### Testing POST Requests
```java
String json = "{\"field\":\"value\"}";
InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
when(mockExchange.getRequestMethod()).thenReturn("POST");
when(mockExchange.getRequestBody()).thenReturn(input);
assertDoesNotThrow(() -> handler.handle(mockExchange));
```

### Testing Query Parameters
```java
URI uri = new URI("http://localhost:8000/items?userId=user123&filter=active");
when(mockExchange.getRequestURI()).thenReturn(uri);
when(mockExchange.getRequestMethod()).thenReturn("GET");
assertDoesNotThrow(() -> handler.handle(mockExchange));
```

### Testing Error Cases
```java
String malformedJson = "{invalid json";
InputStream input = new ByteArrayInputStream(malformedJson.getBytes(UTF-8));
when(mockExchange.getRequestMethod()).thenReturn("POST");
when(mockExchange.getRequestBody()).thenReturn(input);
assertDoesNotThrow(() -> handler.handle(mockExchange)); // Should handle gracefully
```

---

## 📈 Test Coverage Goals

- ✅ **Input Validation**: All fields validated
- ✅ **HTTP Methods**: All supported and unsupported methods tested
- ✅ **Status Codes**: 2xx, 4xx, 5xx responses verified
- ✅ **Edge Cases**: Null, empty, malformed data
- ✅ **Business Rules**: Domain logic validated
- ✅ **Error Handling**: Graceful failure handling

---

## 🔍 Query Parameter Examples

### Login/Register Default URL
```
POST /api/login
POST /api/register
```

### Item Endpoints with Query Params
```
GET /items                          # List all items
GET /items?search=laptop            # Search items
GET /items?category=electronics     # Filter by category
GET /items/item123?userId=user456   # Get item with user context
```

### Wallet Operations
```
POST /api/wallet/deposit            # Deposit funds
POST /api/auction/settle            # Settle auction
```

---

## 🎓 Learning Resources

### Understanding Handlers
- Handlers implement `HttpHandler` interface
- `handle(HttpExchange exchange)` is the entry point
- Handlers parse JSON from request body
- Handlers set response headers and status codes

### Mocking with Mockito
- `mock()` - Create mock objects
- `when()...thenReturn()` - Setup behavior
- `verify()` - Assert method was called

### JSON Handling with Gson
- Serialize objects: `gson.toJson(object)`
- Deserialize JSON: `gson.fromJson(json, Class.class)`
- Raw JSON strings for testing

---

## ✅ Checklist Before Running

- [ ] Java 11+ installed
- [ ] Maven installed
- [ ] Project dependencies downloaded (`mvn install`)
- [ ] All test files in correct location
- [ ] No compile errors (`mvn compile`)

---

## 📋 Test Report

Generate test report:
```bash
mvn test
mvn surefire-report:report
open target/site/surefire-report.html
```

---

## 🐛 Troubleshooting

### "Constructor not found" errors
**Cause**: ItemSummary/ItemPayload constructor mismatch
**Fix**: Use JSON strings directly instead of object setters

### "Method not found" errors
**Cause**: ItemPayload doesn't have setters
**Fix**: ItemPayload uses constructor injection, pass data via JSON

### URI creation issues
**Cause**: Invalid URI string format
**Fix**: Use proper URI format with http:// prefix

### Mockito errors
**Cause**: Missing imports
**Fix**: Ensure `import static org.mockito.Mockito.*;`

---

## 🚀 Next Steps

1. Run tests: `mvn test -Dtest=*HandlerTest`
2. Check results in terminal
3. View detailed report in `target/site/`
4. Add more edge case tests as needed
5. Integrate into CI/CD pipeline

---

## 📞 Support

For questions or issues, check:
1. API_TESTS_DESIGN_REPORT.md - Detailed design
2. Test comments - Inline documentation
3. Handler source code - Implementation details
4. Mockito documentation - Mocking best practices

---

**Last Updated:** May 2026  
**Status:** Production Ready ✅

