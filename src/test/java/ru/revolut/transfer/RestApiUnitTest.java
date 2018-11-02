package ru.revolut.transfer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;


public class RestApiUnitTest {

    @Before
    public void startServer(){
        MoneyTransferAPI.start();
    }

    @After
    public void stopServer(){
        MoneyTransferAPI.stop();
		try{
			// Wait a bit to be sure server resources have been released
			// Otherwise in quick test we could enocunter "Address already in use"
			Thread.sleep(100);
		} catch (Throwable e) {
			e.printStackTrace();
		}
    }

    @Test
    public void simple_account_creation_should_work() {
        int accountNumber = 6458;
        BigDecimal balance = new BigDecimal("34.35");
        // create an account
        when().post("/account/{accountNumber}/{balance}", accountNumber,balance).
            then().statusCode(200);
        // retrieve it back and check
        when().get("/account/{accountNumber}", accountNumber).
            then().
                statusCode(200).
                body("number", equalTo(accountNumber),
                        "balance",equalTo(balance.floatValue())
                );
    }

    @Test
    public void account_creation_with_illegal_arguments_should_fail() {
        // create account with more then 2 digits after decimal point in balance value
        when().post("/account/6458/34.355").
            then().statusCode(500);
        // create account with negative balance
        when().post("/account/6458/-34.35").
            then().statusCode(500);
        // create account with negative account number
            when().post("/account/-6458/34.35").
        then().statusCode(500);
        // create account with negative account number and balance
        when().post("/account/-6458/-34.35").
            then().statusCode(500);
    }

    @Test
    public void request_for_non_existing_account_should_fail(){
        // retrieve non existing account
        when().get("/account/300").
                then().statusCode(500);
    }

    @Test
    public void simple_money_transfer_should_work() {
        // create account #100
        when().post("/account/100/120.45").
                then().statusCode(200);
        // create account #200
        when().post("/account/200/2678.79").
                then().statusCode(200);
        // transfer 11.17 units of money from account #200 to acount #100
        when().put("/transfer/200/100/11.17").
                then().statusCode(200);
        // retrieve them back and check
        when().get("/account/200").
                then().
                    statusCode(200).
                    body("number", equalTo(200),
                            "balance",equalTo(2667.62f)
                    );
        when().get("/account/100").
                then().
                    statusCode(200).
                    body("number", equalTo(100),
                            "balance",equalTo(131.62f)
                    );
    }

    @Test
    public void money_transfer_of_all_funds_should_work() {
        // create account #350
        when().post("/account/350/120.45").
                then().statusCode(200);
        // create account #380
        when().post("/account/380/12.36").
                then().statusCode(200);
        // transfer money from account #350 to acount #380
        when().put("/transfer/350/380/120.45").
                then().statusCode(200);
        // retrieve them back and check
        when().get("/account/350").
                then().statusCode(200).
                body("number", equalTo(350),
                        "balance",equalTo(0.0f)
                );
        when().get("/account/380").
                then().
                statusCode(200).
                body("number", equalTo(380),
                        "balance",equalTo(132.81f)
                );
    }

    @Test
    public void money_transfer_in_amount_that_exceeds_balance_should_fail() {
        // create account #250 with balance 120.45
        when().post("/account/250/120.45").
                then().statusCode(200);
        // create to account #280
        when().post("/account/280/0.00").
                then().statusCode(200);
        // transfer 120.46 units of money from account #250 to acount #280
        when().put("/transfer/250/280/120.46").
                then().statusCode(500);
    }

    @Test
    public void money_transfer_with_illegal_arguments_should_fail() {
        // create simple account #150
        when().post("/account/150/120.45").
                then().statusCode(200);
        // trying to transfer from #150 to non-existing acount #180
        when().put("/transfer/150/180/11.17").
                then().statusCode(500);
        // trying to transfer from #150 to non-existing acount #180
        when().put("/transfer/180/150/11.17").
                then().statusCode(500);
        // trying to transfer from #150 to non-existing acount #180
        when().put("/transfer/170/180/11.17").
                then().statusCode(500);
    }

}
