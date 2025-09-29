package project.pp_backend.exception;

public class BasicErrorMessage extends RuntimeException {
    public BasicErrorMessage(String message) {
        super(message);
    }
}
