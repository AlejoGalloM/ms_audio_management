package co.com.example.model.transcribe;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Audio {
    private Integer jobId;
    private String fileName;
    private String transcript;
}
