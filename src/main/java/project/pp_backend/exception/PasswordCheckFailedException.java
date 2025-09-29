package project.pp_backend.exception;

public class PasswordCheckFailedException extends RuntimeException {
    public PasswordCheckFailedException(String message) {
        super(message);

    }
}
