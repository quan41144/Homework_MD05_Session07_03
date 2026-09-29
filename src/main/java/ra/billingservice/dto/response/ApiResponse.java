package ra.billingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApiResponse <T>{
    private Boolean success;
    private String message;
    private T data;
    private T error;
    private LocalDateTime timestamp;
}
