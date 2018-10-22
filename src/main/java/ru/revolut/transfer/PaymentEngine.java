package ru.revolut.transfer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * The class where implement internal logic of payments.
 */
public class PaymentEngine {

    // Singleton
    private static final PaymentEngine engine = new PaymentEngine();
    public static PaymentEngine getInstance(){ return engine; }
    private PaymentEngine() {}

    /**
     * Payment Engine general ledger.
     */
    private Map<Integer, Account> ledger = new HashMap<>();

    /**
     * Create new account.
     * @param number new account number
     * @param balance account initial balance
     * @return instance of AccountView class that represents created account
     * @throws PaymentEngineException in case of illegall arguments or when account with the same number already exists
     */
    public AccountView createAccount(int number, BigDecimal balance) throws PaymentEngineException {
        if (number<0 || balance.compareTo(BigDecimal.ZERO)<0) throw new PaymentEngineException("Account number and balance must be non-negative");
        if (balance.stripTrailingZeros().scale()>2) throw new PaymentEngineException("Precision of balance must not exceed 2 digits after decimal point");
        Account account;
        synchronized (ledger){
            if (ledger.get(number)!=null)
                throw new PaymentEngineException("Account already exists");
            account = new Account(number,balance.multiply(new BigDecimal("100.00")).longValue());
            ledger.put(number,account);
        }
        return account.toAccountView();
    }

    /**
     * Retrieve an account view
     * @param number number of account ot retrieve it view
     * @return instance of AccountView that represents the account
     * @throws PaymentEngineException in case of illegal account number or when account with specified nuber is not found
     */
    public AccountView getAccountView(int number) throws PaymentEngineException {
        return getAccount(number).toAccountView();
    }

    /**
     * Internal method to retrieve account instance
     * @param number number of account to retrieve
     * @return instance of Account class
     * @throws PaymentEngineException if specified account nu,ber is negative or account not found
     */
    private Account getAccount(int number) throws PaymentEngineException {
        if (number<0) throw new PaymentEngineException("Account number must be non-negative");
        Account account = ledger.get(number);
        if (account==null)
            throw new PaymentEngineException("Account not found");
        return account;
    }

    /**
     * Money transfer operation between two accounts.
     * @param accountFromNumber number of account fto debit
     * @param accountToNumber number of account to credit
     * @param amount money amount ot transfer
     * @throws PaymentEngineException in case of illegal arguments or when acounts with specified numbers do not exist
     */
    public void transfer(int accountFromNumber, int accountToNumber, BigDecimal amount) throws PaymentEngineException {
        if (amount.compareTo(BigDecimal.ZERO)<0) throw new PaymentEngineException("Non-positive money amount transfer is not allowed");
        if (amount.scale()>2) throw new PaymentEngineException("Precision of balance must not exceed 2 digits after decimal point");
        if (accountFromNumber == accountToNumber) throw new PaymentEngineException("Source account number must not be the same as destination account number");
        Account accountFrom = getAccount(accountFromNumber);
        Account accountTo = getAccount(accountToNumber);
        long amountInLong = amount.multiply(new BigDecimal("100")).longValue();

        // Uncomment me to behold the power of concurrency tests
//        if (accountFrom.getBalance() < amountInLong)
//                throw new PaymentEngineException("Insufficient funds");
//        Thread.yield();
//        accountFrom.setBalance(accountFrom.getBalance()-amountInLong);
//        accountTo.setBalance(accountTo.getBalance()+amountInLong);

        // Might be simple and efficient as a brick, but NO!
//        synchronized (ledger){
//            if (accountFrom.getBalance() < amountInLong)
//                throw new PaymentEngineException("Insufficient funds");
////            Thread.yield(); // FIXME remove this statement. It is just to provoke Race Condition, if any, and catch it in concurrency tests.
//            accountFrom.setBalance(accountFrom.getBalance()-amountInLong);
//            accountTo.setBalance(accountTo.getBalance()+amountInLong);
//        }

        // And How Do You Like It, Elon Musk?
        // Should expose non-blocking and much higher performance on a concurrent transfer request for different accounts.
        // Apply resource ordering technique to prevent dead locks.
        Account accountWithLowerNumber = accountFromNumber<accountToNumber?accountFrom:accountTo;
        Account accountWithGreaterNumber = accountFromNumber>accountToNumber?accountFrom:accountTo;
        synchronized (accountWithLowerNumber){
            synchronized (accountWithGreaterNumber){
                if (accountFrom.getBalance() < amountInLong)
                    throw new PaymentEngineException("Insufficient funds");
//                Thread.yield(); // FIXME remove this statement. It is just to provoke Race Condition, if any, and catch it in concurrency tests.
                accountFrom.setBalance(accountFrom.getBalance()-amountInLong);
                accountTo.setBalance(accountTo.getBalance()+amountInLong);
            }
        }


    }

    /**
     * Reset the state of ledger.
     */
    public void reset(){
        ledger = new HashMap<>();
    }

}
