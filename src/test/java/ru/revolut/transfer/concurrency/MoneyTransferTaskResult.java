package ru.revolut.transfer.concurrency;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Container for money transfer task result.
 */
@Getter
@Setter
@AllArgsConstructor
public class MoneyTransferTaskResult {
    private int transfersCounter;
    private int successfullTransfersCounter;
    private BigDecimal overallTransferredMoneyAmount;
    /**
     * Map of exceptions thrown with the number of iteration as a key.
     * '0' key is for general exception.
     * '1' key and further is for exception on transfers with corresponded number.
     */
    private Map<Integer,Throwable> exceptions;
}
