package payment.gateway.exception;

public class LedgerImbalanceException extends RuntimeException {
    public LedgerImbalanceException(String message){
        super(message);
    }
}
