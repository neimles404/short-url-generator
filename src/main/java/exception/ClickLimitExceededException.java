package exception;

public class ClickLimitExceededException extends Exception {
    public ClickLimitExceededException(String message) {
        super(message);
    }
}
