package net.dsa.scitHub.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 성별을 나타내는 Enum
 * M: 남성, F: 여성, N: 선택 안함/기본값
 */
@Getter
@RequiredArgsConstructor
public enum Gender {
    /** 남성 */
    M("Man"),
    /** 여성 */
    F("Woman"),
    /** 선택 안함/기본값 */
    N("Not Specified");

    private final String displayName;
}
