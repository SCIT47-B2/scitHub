package net.dsa.scitHub.entity.schedule;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.User;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "dday")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class Dday {
    
    /** 디데이 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dday_id")
    private Integer ddayId;
    
    /** 디데이 등록 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /** 디데이 날짜 */
    @Column(name = "dday", nullable = false)
    private LocalDate dday;
    
    /** 디데이 제목 */
    @Column(name = "title", nullable = false, length = 100)
    private String title;
    
    /** 디데이 고정 여부 */
    @Builder.Default
    @Column(name = "is_pinned", nullable = false)
    private boolean isPinned = false;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dday)) return false;
        Dday dday1 = (Dday) o;
        return Objects.equals(ddayId, dday1.ddayId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ddayId);
    }
}
