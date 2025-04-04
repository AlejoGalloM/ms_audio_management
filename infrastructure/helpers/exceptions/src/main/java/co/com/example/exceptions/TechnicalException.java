package co.com.example.exceptions;

import co.com.example.exceptions.message.TechnicalExceptionMessage;
import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {
    private final TechnicalExceptionMessage technicalExceptionMessage;

    public TechnicalException(String message) {
        super(message);
        this.technicalExceptionMessage = null;
    }

    public TechnicalException(TechnicalExceptionMessage technicalExceptionMessage) {
        super(technicalExceptionMessage.getMessage());
        this.technicalExceptionMessage = technicalExceptionMessage;
    }
}
