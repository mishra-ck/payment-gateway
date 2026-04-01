package payment.gateway.domain.model;

import payment.gateway.config.constants.Constants;

public sealed interface PaymentStatus permits
        PaymentStatus.Pending,
        PaymentStatus.Processing,
        PaymentStatus.Settled,
        PaymentStatus.Failed {

    String code();
    String description();
    boolean isTerminal();

    record Pending() implements PaymentStatus{
        @Override
        public String code() {
            return Constants.PaymentStatus.PENDING;
        }
        @Override
        public String description() {
            return "Payment received, awaiting processing";
        }
        @Override
        public boolean isTerminal() {
            return false;
        }
    }
    record Processing() implements PaymentStatus{
        @Override
        public String code() {
            return Constants.PaymentStatus.PROCESSING;
        }
        @Override
        public String description() {
            return "Debit applied, credit in progress";
        }
        @Override
        public boolean isTerminal() {
            return false;
        }
    }
    record Settled() implements PaymentStatus{

        @Override
        public String code() {
            return Constants.PaymentStatus.SETTLED;
        }
        @Override
        public String description() {
            return "Payment fully settled and ledger balanced";
        }
        @Override
        public boolean isTerminal() {
            return false;
        }
    }

    record Failed(String reason,String step) implements PaymentStatus{
        @Override
        public String code() {
            return Constants.PaymentStatus.FAILED;
        }
        @Override
        public String description() {
            return "Payment failed: " + reason;
        }
        @Override
        public boolean isTerminal() {
            return true ;
        }
    }

    /** -------- factory method ---------- */
    static PaymentStatus pending()    { return new Pending(); }
    static PaymentStatus processing() { return new Processing(); }
    static PaymentStatus settled()    { return new Settled(); }
    static PaymentStatus failed(String reason,String step) {
        return new Failed(reason,step);
    }

    static PaymentStatus fromCode(String code){
        return switch (code){
            case "pending" -> pending();
            case "processing" -> processing();
            case "settled" -> settled();
            default ->  throw new IllegalArgumentException("Unknown status: "+ code);
        };
    }
    default boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case Pending    p -> next instanceof Processing || next instanceof Failed;
            case Processing p -> next instanceof Settled   || next instanceof Failed;
            case Settled    s -> false;
            case Failed     f -> false;
        };
    }

}
