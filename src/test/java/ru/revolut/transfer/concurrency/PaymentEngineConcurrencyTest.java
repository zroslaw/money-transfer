package ru.revolut.transfer.concurrency;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.revolut.transfer.AccountView;
import ru.revolut.transfer.PaymentEngine;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * A few naive and exhaustive attempts to catch race conditions, deadlocks and these kind of concurrency issues.
 * It is better to do it with a bare PaymentEngine instance instead of dealing with it through REST API
 * as it increases the possibility to catch race conditions and other concurrency problems.
 */
public class PaymentEngineConcurrencyTest {

    @Before
    public void initPaymentEngine(){

    }

    @After
    public void resetPaymentEngine(){
        PaymentEngine.getInstance().reset();
    }

    @Test
    public void cuncurrent_transfers_should_not_break_ledger_consistency() throws Throwable {

        // setting up test parameters
        int numberOfTransferThreads = 10;
        int numberOfTransfersPerOneThread = 100000;
        BigDecimal initialMoneyAmount = new BigDecimal("1000000.00");
        int initialAccountNumber = 0;
        BigDecimal moneyAmountPerOneTransfer = new BigDecimal("0.33");

        // Creating initial source account
        PaymentEngine engine = PaymentEngine.getInstance();
        engine.createAccount(initialAccountNumber,initialMoneyAmount);

        // initializing set of concurrent transfer tasks
        Collection<MoneyTransferTask> transferTasks = new HashSet<>();
        for (int i=1;i<=numberOfTransferThreads;i++){
            transferTasks.add(
                    new MoneyTransferTask(
                            initialAccountNumber,
                            initialAccountNumber+i,
                            numberOfTransfersPerOneThread,
                            moneyAmountPerOneTransfer
                    )
            );
        }

        // initializing executor service and start all of our tasks
        ExecutorService executorService =  Executors.newFixedThreadPool(numberOfTransferThreads);
        List<Future<MoneyTransferTaskResult>> futureTasksResults = executorService.invokeAll(transferTasks);

        // shutting executor service down and wait for its finish
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        assertTrue("Concurrent money transfer tasks took too long time to finish", executorService.isTerminated());

        /*
         *   Now lets check the results:
         */

        // first of all, all tasks should succeed
        for (Future<MoneyTransferTaskResult> futureTaskResult:futureTasksResults){
            assertTrue(futureTaskResult.isDone());
            MoneyTransferTaskResult result = futureTaskResult.get();
            assertTrue(result.getExceptions().isEmpty());
            assertEquals(result.getSuccessfullTransfersCounter(), numberOfTransfersPerOneThread);
        }

        // final overall money amount in ledger and should be equal to initial one
        BigDecimal finalOverallMoneyAmount;
        finalOverallMoneyAmount = new BigDecimal("0.00");
        for(int i=initialAccountNumber;i<numberOfTransferThreads+1;i++){
            AccountView account = engine.getAccountView(i);
            // System.out.println(account.getNumber()+"\t: "+account.getBalance());
            finalOverallMoneyAmount = finalOverallMoneyAmount.add(account.getBalance());
        }
        assertEquals("Ledger consistency is broken!",initialMoneyAmount,finalOverallMoneyAmount);

        // balance of initial account should be equal to (initialMoneyAmount - numberOfTransfersPerOneThread*numberOfTransferThreads*moneyAmountPerOneTransfer)
        assertEquals(
                "Initial account balance is incorrect",
                engine.getAccountView(initialAccountNumber).getBalance().stripTrailingZeros(),
                initialMoneyAmount.subtract(
                            new BigDecimal(numberOfTransfersPerOneThread * numberOfTransferThreads).multiply(moneyAmountPerOneTransfer)
                        ).stripTrailingZeros()
        );

        // and finally, just to be sure..
        BigDecimal expectedDestinationAccountBalance = moneyAmountPerOneTransfer.multiply(new BigDecimal(numberOfTransfersPerOneThread));
        for(int i=initialAccountNumber+1;i<numberOfTransferThreads+1;i++){
            AccountView account = engine.getAccountView(i);
            assertEquals("One of destination account has incorrect balance",
                    account.getBalance().stripTrailingZeros(),
                    expectedDestinationAccountBalance.stripTrailingZeros());
        }


    }

    @Test
    public void cuncurrent_transfers_should_not_produce_negative_balance() throws Throwable {

        // setting up test parameters
        int numberOfTransferThreads = 10;
        int numberOfAttemptsToCatchNegativeBalance = 1000;
        BigDecimal initialMoneyAmount = new BigDecimal("1.00");
        int initialAccountNumber = 0;
        BigDecimal moneyAmountPerOneTransfer = new BigDecimal("0.60");

        PaymentEngine engine = PaymentEngine.getInstance();

        // a lot of attempts to concurrently withdraw larger part of money from an initial account
        for (int j=0;j<numberOfAttemptsToCatchNegativeBalance;j++) {

            // reset engine state in the beginning of each attempt
            engine.reset();

            // create initial account
            engine.createAccount(initialAccountNumber, initialMoneyAmount);

            // initializing set of concurrent transfer tasks
            Collection<MoneyTransferTask> transferTasks = new HashSet<>();
            for (int i = 1; i <= numberOfTransferThreads; i++) {
                transferTasks.add(
                        new MoneyTransferTask(
                            initialAccountNumber,
                            initialAccountNumber+i,
                            1,
                            moneyAmountPerOneTransfer
                        )
                );
            }

            // initializing executor service and start all of our tasks
            ExecutorService executorService =  Executors.newFixedThreadPool(numberOfTransferThreads);
            List<Future<MoneyTransferTaskResult>> futureTasksResults = executorService.invokeAll(transferTasks);

            // shutting executor service down and wait for its tasks to finish
            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.SECONDS);
            assertTrue("Concurrent money transfer tasks took too long time to finish", executorService.isTerminated());

            /*
             * Check the results
             */

            // initial account should have positive balance that equals to (initialMoneyAmount-moneyAmountPerOneTransfer)
            BigDecimal initialAccountBalance = engine.getAccountView(initialAccountNumber).getBalance();
            assertTrue("Negative initial account balance: "+initialAccountBalance,initialAccountBalance.floatValue()>0f);
            assertEquals("Incorrect initial account balance", initialMoneyAmount.subtract(moneyAmountPerOneTransfer).stripTrailingZeros(),initialAccountBalance.stripTrailingZeros());

            // Only one task should actually succeed
            int successCounter = 0;
            for (Future<MoneyTransferTaskResult> futureTaskResult:futureTasksResults){
                assertTrue(futureTaskResult.isDone());
                MoneyTransferTaskResult result = futureTaskResult.get();
                // each task must succeed or fail
                // in case of success there should not be any exception
                // in case of fail there should be only one exception
                assertTrue("Failed tasks should have the only one exception",
                        result.getSuccessfullTransfersCounter()==0?result.getExceptions().size()==1:result.getExceptions().size()==0);
                assertTrue("Taks with zero exceptions should have the only one succeessfull transfer",
                        result.getExceptions().size()==0?result.getSuccessfullTransfersCounter()==1:result.getSuccessfullTransfersCounter()==0);
                if (result.getSuccessfullTransfersCounter()==1)
                    successCounter++;
            }
            assertEquals("Only one transfer thread should succeed",1, successCounter);
        }

    }

    @Test
    public void cyclic_concurrent_transfers_should_not_produce_deadlock(){
        /*
        Here must be a test to provoke dead lock condition.
        It might occur when Payment Engine incorrectly synchronizes on a particular account instances.
        Multiple attempts of concurrent cyclic transfers between several accounts may expose the deadlock.
         */
        assertTrue("The idea is cool but there is no time to implement it", true);
    }

}
