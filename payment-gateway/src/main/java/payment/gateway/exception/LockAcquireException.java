package payment.gateway.exception;

public class LockAcquireException extends Exception{
    public LockAcquireException(String message) { super(message); }
    public LockAcquireException(String message, Throwable cause) { super(message, cause); }
}
