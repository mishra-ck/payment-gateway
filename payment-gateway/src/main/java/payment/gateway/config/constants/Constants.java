package payment.gateway.config.constants;

public interface Constants {

     interface Endpoint{
         String BASE_PATH = "/api";
         String VERSION_V1 = "/v1";
         String PAYMENTS = "payments";
    }

     interface PaymentStatus{
         String INITIATED = "payment-initiated";
         String PENDING = "payment-pending";
         String PROCESSING = "payment-processing";
         String DEBITED = "payment-debited";
         String CREDITED = "payment-credited";
         String SETTLED = "payment-settled";
         String FAILED = "payment-failed";
         String COMPENSATED_DEBIT = "compensated-debit";
    }
     interface Lock{
        String ACCOUNT_LOCK_PREFIX = "account:lock";
        String PAYMENT_LOCK_PREFIX = "payment:lock";
        long WAIT_TIMEOUT_SECONDS = 5;
        long LEASE_SECONDS = 30;
    }

     String SYSTEM = "system";

}
