package net.dsa.scitHub.enums;

/**
 * 회사 산업 분야를 나타내는 Enum
 */
public enum Industry {
    /** IT 업계 */
    IT("IT"),
    /** 제조업 */
    MANUFACTURING("製造"),
    /** 금융업 */
    FINANCE("金融"),
    /** 산업 */
    INDUSTRIAL("産業"),
    /** 통신업 */
    TELECOM("通信"),
    /** 서비스업 */
    SERVICE("サービス"),
    /** 기타 */
    OTHER("その他");
    
    private final String displayName;
    
    Industry(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 표시명으로 Industry enum을 찾는 메서드
     * @param displayName 한글 표시명
     * @return 해당하는 Industry enum
     */
    public static Industry fromDisplayName(String displayName) {
        for (Industry industry : values()) {
            if (industry.displayName.equals(displayName)) {
                return industry;
            }
        }
        throw new IllegalArgumentException("Unknown display name: " + displayName);
    }

}
