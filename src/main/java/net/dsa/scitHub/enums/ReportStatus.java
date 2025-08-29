package net.dsa.scitHub.enums;

/**
 * 신고 상태를 나타내는 Enum
 * PENDING: 대기중, RESOLVED: 처리완료, REJECTED: 반려
 */
public enum ReportStatus {
    /** 대기중 */
    PENDING,
    /** 처리완료 */
    RESOLVED,
    /** 반려 */
    REJECTED
}
