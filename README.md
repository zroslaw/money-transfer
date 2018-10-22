
## Money Transfer REST API Server

To run tests `mvn clean compile test`.

To run REST API server on port 8080  `mvn clean compile exec:java`.

## REST API Methods
**Note:** Account number is a non-negative integer value. Account balance is a positive decimal value with a precision that must not exceed 2 digit after the point.

1. Create new account with specified number and initial balance
   ```
   POST http://localhost:8080/acount/{accountNumber}/{initialBalance}
   ```
2. Retrieve an account by its number
   ```
   GET http://localhost:8080/acount/{accountNumber}
   ```
3. Transfer specified money amount from one accout to another
   ```
   PUT http://localhost:8080/transfer/{fromAccountNumber}/{toAccountNumber}/{moneyAmnountToTransfer}
   ```

