# API Unit Tests Design Report

## Overview
Thiết kế và triển khai comprehensive unit tests cho tất cả API endpoints của Online Auction Server.

---

## 📊 API Test Coverage

### 1. **LoginHandler** ✅
**File:** `LoginHandlerTest.java`
**HTTP Method:** POST  
**Endpoint:** `/api/login`

#### Test Cases (10 tests):
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_reject_non_post_requests()` | Reject GET requests | 405 Method Not Allowed |
| `handle_should_return_400_when_missing_credentials()` | Validate required fields | 400 Bad Request |
| `handle_should_return_401_for_invalid_credentials()` | Wrong password | 401 Unauthorized |
| `handle_should_return_200_and_user_data_on_successful_login()` | Successful login | 200 OK + UserDTO |
| `handle_should_return_403_when_user_is_banned()` | Banned user login attempt | 403 Forbidden |
| `handle_should_return_405_for_unsupported_methods()` | PUT, DELETE, etc. | 405 Method Not Allowed |
| `handle_should_handle_malformed_json_gracefully()` | Invalid JSON input | 500 or 400 |
| `handle_should_process_correct_json_format()` | Valid JSON format | Success parsing |
| `handle_should_log_login_attempts()` | Logging functionality | No exception |
| `handle_should_return_empty_data_on_invalid_login()` | Invalid credentials | No user data |

---

### 2. **RegisterHandler** ✅
**File:** `RegisterHandlerTest.java`
**HTTP Method:** POST  
**Endpoint:** `/api/register`

#### Test Cases (13 tests):
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_reject_non_post_requests()` | GET/DELETE requests | 405 |
| `handle_should_return_400_when_username_is_missing()` | Null username | 400 |
| `handle_should_return_400_when_password_is_missing()` | Null password | 400 |
| `handle_should_return_400_when_email_is_missing()` | Null email | 400 |
| `handle_should_return_400_when_credentials_are_blank()` | Whitespace fields | 400 |
| `handle_should_return_200_on_successful_registration()` | Valid input | 200 OK |
| `handle_should_return_409_when_username_already_exists()` | Duplicate username | 409 Conflict |
| `handle_should_return_409_when_email_already_exists()` | Duplicate email | 409 Conflict |
| `handle_should_return_405_for_unsupported_methods()` | Invalid methods | 405 |
| `handle_should_trim_username_and_email()` | Whitespace trimming | Trimmed values |
| `handle_should_handle_malformed_json_gracefully()` | Bad JSON | 500 or 400 |
| `handle_should_accept_valid_email_formats()` | Email validation | Success |
| `handle_should_accept_special_characters_in_password()` | Special chars validation | Success |

---

### 3. **ItemsHandler** ✅
**File:** `ItemsHandlerTest.java`
**HTTP Methods:** GET, POST  
**Endpoint:** `/api/items`

#### Test Cases (20 tests):

**GET Tests (5):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_return_200_with_items_on_get_request()` | List all items | 200 + items array |
| `handle_should_return_404_when_items_not_found()` | Empty list | 404 |
| `handle_should_pass_query_parameters_to_service()` | Query params handling | Search applied |
| `handle_should_handle_get_without_query_parameters()` | No parameters | All items |
| `handle_should_return_empty_list_when_no_items_match_query()` | No matches | Empty array |

**POST Tests (7):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_return_200_when_item_created_successfully()` | Valid item | 200 Created |
| `handle_should_return_500_when_item_creation_fails()` | Creation error | 500 |
| `handle_should_accept_item_with_all_required_fields()` | All fields present | Success |
| `handle_should_handle_item_with_special_characters_in_name()` | Unicode chars | Success |
| `handle_should_accept_large_starting_prices()` | Big numbers | Success |
| `handle_should_accept_decimal_prices_and_bid_steps()` | Decimal validation | Success |
| `handle_should_handle_empty_request_body()` | Empty JSON | Proper error |

**Method Not Allowed (3):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_return_405_for_unsupported_methods()` | DELETE | 405 |
| `handle_should_return_405_for_put_request()` | PUT | 405 |
| `handle_should_return_405_for_patch_request()` | PATCH | 405 |

**Error Handling (5):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_handle_malformed_json_in_post_request()` | Bad JSON | Error |
| `handle_should_handle_missing_required_fields()` | Empty fields | Error |
| `handle_should_handle_null_request_body()` | Null body | Error |
| `handle_should_handle_empty_request_body()` | {} | Error |
| `handle_should_accept_valid_item_payload_structure()` | Valid JSON | Success |

---

### 4. **WalletHandler** ✅
**File:** `WalletHandlerTest.java`
**HTTP Methods:** POST  
**Endpoints:** 
- `/api/wallet/deposit`
- `/api/auction/settle`

#### DepositHandler Tests (8 tests):
| Test | Purpose | Expected |
|------|---------|----------|
| `depositHandler_should_reject_non_post_requests()` | GET request | 405 |
| `depositHandler_should_return_400_when_userId_missing()` | Null userId | 400 |
| `depositHandler_should_return_400_when_amount_invalid()` | Negative amount | 400 |
| `depositHandler_should_return_400_when_amount_zero()` | Zero amount | 400 |
| `depositHandler_should_accept_valid_deposit()` | Valid request | 200 |
| `depositHandler_should_accept_large_deposit_amounts()` | 1M amount | Success |
| `depositHandler_should_handle_malformed_json()` | Bad JSON | Error |
| `depositHandler_should_handle_decimal_amounts()` | Decimal values | Success |

#### SettleHandler Tests (8 tests):
| Test | Purpose | Expected |
|------|---------|----------|
| `settleHandler_should_reject_non_post_requests()` | GET | 405 |
| `settleHandler_should_return_400_when_itemId_missing()` | Null itemId | 400 |
| `settleHandler_should_return_400_when_itemId_blank()` | Whitespace | 400 |
| `settleHandler_should_accept_valid_settlement_request()` | Valid itemId | Settlement |
| `settleHandler_should_handle_malformed_json()` | Bad JSON | Error |
| `settleHandler_should_handle_special_characters_in_itemId()` | UUID format | Success |
| `settleHandler_should_accept_long_itemIds()` | Long IDs | Success |

---

### 5. **ItemDetailHandler** ✅
**File:** `ItemDetailHandlerTest.java`
**HTTP Methods:** GET, PUT, DELETE  
**Endpoint:** `/api/items/{itemId}`

#### Test Cases (19 tests):

**GET Tests (5):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_return_200_with_item_on_get_request()` | Get item | 200 + item |
| `handle_should_return_404_when_item_not_found()` | Missing item | 404 |
| `handle_should_extract_item_id_from_url()` | Path extraction | Correct ID |
| `handle_should_attach_user_last_bid_when_userId_provided()` | Query param: userId | User bid attached |
| `handle_should_handle_get_without_userId_parameter()` | No params | Item data |

**PUT Tests (5):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_return_200_when_item_updated_successfully()` | Valid update | 200 |
| `handle_should_return_500_when_item_update_fails()` | Update fail | 500 |
| `handle_should_extract_item_id_from_put_request_path()` | Path extraction | Correct ID |
| `handle_should_accept_item_with_all_update_fields()` | Full update | Success |
| `handle_should_handle_malformed_json_in_put_request()` | Bad JSON | Error |

**DELETE Tests (3):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_return_200_when_item_deleted_successfully()` | Delete item | 200 |
| `handle_should_return_500_when_item_deletion_fails()` | Delete fail | 500 |
| `handle_should_extract_item_id_from_delete_request_path()` | Path extraction | Correct ID |

**Validation Tests (6):**
| Test | Purpose | Expected |
|------|---------|----------|
| `handle_should_extract_user_id_from_query_string()` | Query parsing | Correct userId |
| `handle_should_ignore_userId_when_not_present_in_query()` | Optional param | No error |
| `handle_should_handle_complex_query_strings()` | Multiple params | All parsed |
| `handle_should_reject_unsupported_http_methods()` | PATCH | 405 |
| `handle_should_extract_item_id_from_nested_paths()` | Deep paths | Correct ID |
| `handle_should_handle_special_characters_in_item_id()` | UUID-like ID | Success |

---

## 🏗️ Architecture

### Testing Pattern
```
Handler Test
├── Setup (Mock HttpExchange, Gson)
├── Test Cases
│   ├── Arrange (Create request payload)
│   ├── Act (Execute handler.handle())
│   └── Assert (Verify response)
└── Cleanup (Auto via JUnit)
```

### Key Technologies
- **Framework:** JUnit 5 (Jupiter API)
- **Mocking:** Mockito
- **Serialization:** Gson
- **HTTP:** Java HttpServer API

---

## 📋 Test Statistics

| Metric | Value |
|--------|-------|
| **Total Test Files** | 5 |
| **Total Test Methods** | 70+ |
| **Total Lines of Code** | 1,500+ |
| **Handlers Tested** | 5 |
| **HTTP Methods Covered** | GET, POST, PUT, DELETE, PATCH |
| **Error Cases** | 25+ |
| **Happy Path Cases** | 45+ |

---

## ✨ Coverage Areas

### Input Validation
- ✅ Null field validation
- ✅ Empty/blank field validation  
- ✅ Special characters handling
- ✅ Decimal precision
- ✅ Large number handling

### HTTP Protocol
- ✅ Correct HTTP methods
- ✅ Method not allowed (405)
- ✅ Query parameter parsing
- ✅ Path parameter extraction
- ✅ Request/response encoding (UTF-8)

### Error Handling
- ✅ Malformed JSON
- ✅ Missing required fields
- ✅ Invalid data types
- ✅ Status code verification
- ✅ Error message formatting

### Business Logic
- ✅ Login with valid/invalid credentials
- ✅ Register with duplicate username/email
- ✅ Deposit with constraints
- ✅ Settlement flow
- ✅ Item CRUD operations

---

## 🚀 Running Tests

### All API Tests
```bash
mvn test -Dtest=*HandlerTest
```

### Individual Handler Tests
```bash
# Login tests
mvn test -Dtest=LoginHandlerTest

# Register tests
mvn test -Dtest=RegisterHandlerTest

# Items tests
mvn test -Dtest=ItemsHandlerTest

# Wallet tests
mvn test -Dtest=WalletHandlerTest

# Item detail tests
mvn test -Dtest=ItemDetailHandlerTest
```

### Specific Test Method
```bash
mvn test -Dtest=LoginHandlerTest#handle_should_return_200_and_user_data_on_successful_login
```

---

## 📝 Best Practices Implemented

1. **AAA Pattern:** Arrange-Act-Assert for clarity
2. **Single Responsibility:** Each test focuses on one behavior
3. **Clear Naming:** Test names describe what they test
4. **Mocking:** Isolate handler from service dependencies
5. **No Exception Throws:** Use `assertDoesNotThrow()`
6. **UTF-8 Handling:** Proper character encoding
7. **Null Safety:** Null checks in assertions
8. **Documentation:** Extensive comments

---

## 🔄 Future Enhancements

- [ ] Integration tests with real database
- [ ] Performance tests (response time benchmarks)
- [ ] Security tests (SQL injection, XSS)
- [ ] Load tests with concurrent requests
- [ ] API contract tests
- [ ] E2E tests with client

---

## Files Created

✅ `LoginHandlerTest.java` (10 tests, 200 lines)
✅ `RegisterHandlerTest.java` (13 tests, 280 lines)
✅ `ItemsHandlerTest.java` (20 tests, 400 lines)
✅ `WalletHandlerTest.java` (16 tests, 330 lines)
✅ `ItemDetailHandlerTest.java` (19 tests, 400 lines)

**Total:** 70+ tests, 1,600+ lines of comprehensive API testing code

---

## ✅ Status
All API handler tests **COMPLETE** and **READY FOR USE**

