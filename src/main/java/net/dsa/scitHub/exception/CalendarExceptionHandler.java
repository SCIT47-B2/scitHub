package net.dsa.scitHub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.controller.CalendarApiController;
import net.dsa.scitHub.controller.CalendarController;

// 이 클래스는 "net.dsa.scitHub.controller.calendar" 패키지와
// 그 하위 패키지에 있는 컨트롤러에만 적용됩니다.
@Slf4j
// 예외 처리 코드를 한 곳으로 모아 관리하기 위한 어노테이션
// assignableTypes 어떤 컨트롤러에서 발생한 예외를 처리할지 범위 지정
@RestControllerAdvice(assignableTypes = {
    CalendarApiController.class,
    CalendarController.class
})
public class CalendarExceptionHandler {
    

    /**
     * @EntityNotFoundException : DB에서 리소스를 찾지 못했을 때 발생하는 예외 처리
     */
    // EntityNotFoundException이 발생했을 때 실행될 메소드
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerEntityNOtFoundException(EntityNotFoundException ex) {
        log.debug("EntityNotFoundException : {} ", ex.getMessage());

        // ErrorResponse 객체 생성
        final ErrorResponse response = new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());

        // ResponseEntity 객체 생성
        // 404 상태 코드와 함께 ErrorResponse를 담아 응답
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 모든 예외를 처리하는 일반적인 예외 핸들러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected Exception : {} ", ex.getMessage(), ex);

        // 일반적인 에러 응답 생성
        final ErrorResponse response = new ErrorResponse("INTERNAL_SERVER_ERROR", "알 수 없는 오류가 발생했습니다");

        // 500 Internal Server Error로 응답
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
