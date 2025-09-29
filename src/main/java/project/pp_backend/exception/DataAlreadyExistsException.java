package project.pp_backend.exception;

//중복되는 데이터가 있을때 발생하는 오류
public class DataAlreadyExistsException extends RuntimeException {
    public DataAlreadyExistsException(String message) {
        super(message);

    }
}
