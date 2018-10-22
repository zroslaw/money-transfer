package ru.revolut.transfer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;

import java.math.BigDecimal;

import java.util.concurrent.TimeUnit;

import static spark.Service.ignite;

/**
 * Money Transfer REST API Server
 */
public class MoneyTransferAPI {

    private static Logger log = LoggerFactory.getLogger(MoneyTransferAPI.class);

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static PaymentEngine engine = PaymentEngine.getInstance();

    private static Service sparkService = null;

    public static void main(String[] args) {
        start();
    }

    /**
     * Start Spark Server and create set of money transfer API routes
     */
    public static void start() {

        if (sparkService!=null) return;

        // Run Spark server on port 8080
        sparkService = ignite().port(8080);

        /*
         * Set of REST API routes
         */

        // POST method to create new account
        sparkService.post("/account/:accountNumber/:accountBalance",(request, response) -> {
            int accountNumber;
            BigDecimal accountBalance;
            try {
                accountNumber = Integer.parseInt(request.params(":accountNumber"));
                accountBalance = new BigDecimal(request.params(":accountBalance"));
            } catch (NumberFormatException e){
                throw new PaymentEngineException("Incorrect number format provided");
            }
            engine.createAccount(accountNumber, accountBalance);
            return "";
        });

        // GET method to retrieve an acoount by its number
        sparkService.get("/account/:accountNumber",(request, response) -> {
            int accountNumber;
            try {
                accountNumber = Integer.parseInt(request.params(":accountNumber"));
            } catch (NumberFormatException e){
                throw new PaymentEngineException("Incorrect number format provided");
            }
            return engine.getAccountView(accountNumber);
        }, gson::toJson);

        // PUT method to transfer money between accounts
        sparkService.put("/transfer/:accountFromNumber/:accountToNumber/:amount",(request, response) -> {
            int accountFromNumber;
            int accountToNumber;
            BigDecimal amount;
            try {
                accountFromNumber = Integer.parseInt(request.params(":accountFromNumber"));
                accountToNumber = Integer.parseInt(request.params(":accountToNumber"));
                amount = new BigDecimal(request.params(":amount"));
            } catch (NumberFormatException e) {
                throw new PaymentEngineException("Incorrect number format provided");
            }
            engine.transfer(accountFromNumber,accountToNumber,amount);
            return "";
        });

        // After-filter to set ContentType header for each response
        sparkService.after((request, response) -> {
            response.type("application/json");
        });

        // Exception handler
        sparkService.exception(PaymentEngineException.class, (exception, request, response) -> {
            log.error(exception.getMessage());
            response.status(500);
            response.body(exception.getMessage());
        });

        // wait a bit for Spark service to be initialized
        sparkService.awaitInitialization();

    }

    /**
     * Stop the REST API server and reset all internal state
     */
    public static void stop(){
        if (sparkService!=null) {
			sparkService.stop();
			try{
				Thread.sleep(100);
			} catch (Throwable e) {}
		}
        sparkService = null;
        engine.reset();
    }

}
