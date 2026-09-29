package ra.billingservice.exception;

public class InvoiceNotCancelException extends RuntimeException {
    public InvoiceNotCancelException(String message) {
        super(message);
    }
}
