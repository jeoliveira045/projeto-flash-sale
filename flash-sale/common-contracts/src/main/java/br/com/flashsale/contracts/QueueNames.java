package br.com.flashsale.contracts;

public final class QueueNames {
    public static final String EXCHANGE = "flash-sale.events";
    public static final String ORDER_CREATED = "order-created";
    public static final String ORDER_COMPENSATION = "order-compensation";
    public static final String ORDER_STATUS_UPDATED = "order-status-updated";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private QueueNames() {
    }
}