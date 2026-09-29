package ra.billingservice.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ra.billingservice.dto.response.ApiResponse;
import ra.billingservice.exception.*;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppointmentNotCancelException.class)
    public ResponseEntity<ApiResponse<?>> handleAppointmentNotCancelException(AppointmentNotCancelException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Lỗi hủy lịch hẹn!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(AppointmentNotCreateException.class)
    public ResponseEntity<ApiResponse<?>> handleAppointmentNotCreateException(AppointmentNotCreateException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Lỗi tạo mới lịch hẹn!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(IdInvalidException.class)
    public ResponseEntity<ApiResponse<?>> handleIdInvalidException(IdInvalidException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Lỗi Id không hợp lệ!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(InvoiceNotCancelException.class)
    public ResponseEntity<ApiResponse<?>> handleInvoiceNotCancelException(InvoiceNotCancelException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Lỗi hủy hóa đơn!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Dữ liệu không tồn tại!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequestException(BadRequestException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Dữ liệu không hợp lệ!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<?>> handleForbiddenException(ForbiddenException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Bạn không có quyền thực hiện thao tác này!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Bạn không có quyền thực hiện thao tác này!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Bạn cần đăng nhập để thực hiện thao tác này!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<?>> handleFeignException(feign.FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                status.is4xxClientError() ? "Lỗi liên thông dịch vụ (Client)!" : "Lỗi liên thông dịch vụ (Server)!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), status);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        return new ResponseEntity<>(new ApiResponse<>(
                false,
                "Lỗi server!",
                null,
                ex.getMessage(),
                LocalDateTime.now()
        ), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
