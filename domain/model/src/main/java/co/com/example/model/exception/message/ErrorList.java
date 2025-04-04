package co.com.example.model.exception.message;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ErrorList {
    private List<Error> errors;

    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder(toBuilder = true)
    public static class Error {
        private String code;
        private String message;
        private String domain;
        private String reason;
    }

}
