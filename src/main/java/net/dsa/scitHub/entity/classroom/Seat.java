package net.dsa.scitHub.entity.classroom;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.Account;

import java.util.Objects;

@Entity
@Table(name = "seat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"classroom", "account"})
public class Seat {
    
    /** 좌석 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Integer seatId;
    
    /** 좌석이 위치한 강의실 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;
    
    /** 좌석 행 번호 */
    @Column(name = "row_no", nullable = false)
    private Integer rowNo;
    
    /** 좌석 열 번호 */
    @Column(name = "col_no", nullable = false)
    private Integer colNo;
    
    /** 좌석을 배정받은 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seat)) return false;
        Seat seat = (Seat) o;
        return Objects.equals(seatId, seat.seatId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(seatId);
    }
}
