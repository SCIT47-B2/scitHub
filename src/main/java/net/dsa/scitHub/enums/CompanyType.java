package net.dsa.scitHub.enums;

/**
 * 회사 유형을 나타내는 Enum
 */
public enum CompanyType {
    /** 자사 개발 */
    IN_HOUSE_DEV("자사 개발"),
    /** 자사 SI */                          
    IN_HOUSE_SI("자사 SI"),
    /** 타사 SI */
    EXTERNAL_SI("타사 SI"),
    /** SES(파견) */
    SES("SES(파견)"),
    /** 기타 */
    OTHER("그 외");
    
    private final String displayName;
    
    CompanyType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 표시명으로 CompanyType enum을 찾는 메서드
     * @param displayName 한글 표시명
     * @return 해당하는 CompanyType enum
     */
    public static CompanyType fromDisplayName(String displayName) {
        for (CompanyType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown display name: " + displayName);
    }
}
