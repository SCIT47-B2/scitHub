package net.dsa.scitHub.enums;

/**
 * 사용자 역할을 나타내는 Enum
 * USER: 일반 사용자, ADMIN: 관리자
 */
public enum Role {
    /** 일반 사용자 */
    USER("ROLE_USER"),
    /** 관리자 */
    ADMIN("ROLE_ADMIN");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
