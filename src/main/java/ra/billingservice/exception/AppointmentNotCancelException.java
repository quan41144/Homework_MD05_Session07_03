package ra.billingservice.exception;

public class AppointmentNotCancelException extends RuntimeException {
    public AppointmentNotCancelException(String message) {
        super(message);
    }
}
