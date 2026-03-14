package payment.gateway.config.constants;

public interface Constants {

    public interface Endpoint{
        public static final String BASE_PATH = "/api";
        public static final String VERSION_V1 = "/v1";
        public static final String PAYMENTS = "payments";
    }

    public interface PaymentStatus{
        public static final String INITIATED = "payment-initiated";
        public static final String PENDING = "payment-pending";
        public static final String PROCESSING = "payment-processing";
        public static final String DEBITED = "payment-debited";
        public static final String CREDITED = "payment-credited";
        public static final String SETTLED = "payment-settled";
        public static final String FAILED = "payment-failed";
        public static final String COMPENSATED_DEBIT = "compensated-debit";
    }

    public static final String SYSTEM = "system";

}
