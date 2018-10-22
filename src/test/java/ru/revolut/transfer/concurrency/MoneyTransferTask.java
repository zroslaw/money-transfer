package ru.revolut.transfer.concurrency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.revolut.transfer.PaymentEngine;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Callable money transfer task.
 */
@Getter
@Setter
@AllArgsConstructor
public class MoneyTransferTask implements Callable<MoneyTransferTaskResult> {

    private int sourceAccountNumber;
    private int destinationAccountNumber;
    private int numberOfTransfersToExecute;
    private BigDecimal moneyAmountPerOneTransfer;

    @Override
    public MoneyTransferTaskResult call() {
        int transfersCounter = 0;
        int successfullTransfersCounter = 0;
        BigDecimal overallTransferredMoneyAmount = new BigDecimal(0);
        Map<Integer,Throwable> exceptions = new HashMap<>();

        PaymentEngine engine = PaymentEngine.getInstance();
        try {
            engine.createAccount(destinationAccountNumber, new BigDecimal(0));
            for (int i = 1; i <= numberOfTransfersToExecute; i++) {
                try {
                    transfersCounter++;
                    engine.transfer(sourceAccountNumber, destinationAccountNumber, moneyAmountPerOneTransfer);
                    successfullTransfersCounter++;
                    overallTransferredMoneyAmount = overallTransferredMoneyAmount.add(moneyAmountPerOneTransfer);
                }catch(Throwable e){
                    exceptions.put(i,e);
                }
                Thread.yield(); // give a chance to over threads to catch RC
            }
        } catch (Throwable e) {
            exceptions.put(0,e);
        }
        return new MoneyTransferTaskResult(
                transfersCounter,
                successfullTransfersCounter,
                overallTransferredMoneyAmount,
                exceptions);
    }
}
