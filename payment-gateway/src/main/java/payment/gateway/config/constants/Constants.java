package payment.gateway.config.constants;

public interface Constants {

    public interface Endpoint{
        public static final String BASE_PATH = "/api";
        public static final String VERSION_V1 = "/v1";
        public static final String PAYMENTS = "payments";
    }

    public interface PaymentStatus{
        public static final String INITIATED = "initiated";
        public static final String PENDING = "pending";
        public static final String PROCESSING = "processing";
        public static final String SETTLED = "settled";
        public static final String FAILED = "failed";
    }

    public static final String SYSTEM = "system";

}
