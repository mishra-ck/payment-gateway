package payment.gateway.config.enums;


public enum TransactionType {
    CREDIT, DEBIT,    // normal payment
    COMPENSATE_DEBIT, COMPENSATE_CREDIT  // payment rollback
}
