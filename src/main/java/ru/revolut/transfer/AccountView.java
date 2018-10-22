package ru.revolut.transfer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A view of account instance for external consumers.
 */
@Getter
@Setter
@AllArgsConstructor
public class AccountView {
    int number;
    BigDecimal balance;
}
