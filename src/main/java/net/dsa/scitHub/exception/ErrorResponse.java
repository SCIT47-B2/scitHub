package net.dsa.scitHub.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
// 에러 정보의 '표준화'를 위한 클래스
public class ErrorResponse {
    
    private final String errorCode;
    private final String message;
}
