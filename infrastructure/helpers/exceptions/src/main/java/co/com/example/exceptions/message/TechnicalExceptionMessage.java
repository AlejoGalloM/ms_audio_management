package co.com.example.exceptions.message;

import co.com.example.model.exception.message.ExceptionMessage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor(force = true, access = lombok.AccessLevel.PRIVATE)
public enum TechnicalExceptionMessage implements ExceptionMessage {
    BAD_REQUEST("AMT0001", "Bad Request", "Missing Information in the request"),
    TECHNICAL_S3_EXCEPTION("AMT0002", "Technical Exception", "An error occurred while interacting with S3"), S3_UPLOAD_ERROR, S3_PUT_OBJECT_FAIL, TECHNICAL_DYNAMODB_EXCEPTION;


    private final String code;
    private final String message;
    private final String description;
}
