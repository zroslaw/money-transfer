package ru.revolut.transfer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Internal Payment Engine account representation.
 */
@Getter
@Setter
@AllArgsConstructor
public class Account {

    /**
     * Simple account number as integer.
     */
    private int number;

    /**
     * Here we need for compact and precise data structure to represent account balance.
     * Primitive 'long' type is good enough as we do not need precision more than 1 cent of money unit,
     * it is compact enough in terms of memory footprint and it will allow fast arithmetics in future.
     */
    private long balance;

    /**
     * Generate account view instance of this account.
     * @return an account view
     */
    public AccountView toAccountView(){
        return new AccountView(number,new BigDecimal(balance).divide(new BigDecimal("100.00")));
    }

}
