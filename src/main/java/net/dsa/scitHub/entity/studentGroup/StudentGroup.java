package net.dsa.scitHub.entity.studentGroup;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.Account;
import net.dsa.scitHub.enums.ClassSection;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "student_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"users"})
public class StudentGroup {
    
    /** 학생 그룹 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_group_id")
    private Integer studentGroupId;
    
    /** 기수 번호 */
    @Column(name = "cohort_no", nullable = false)
    private Integer cohortNo;
    
    /** 반 구분 (A반, B반) */
    @Enumerated(EnumType.STRING)
    @Column(name = "class_section", nullable = false)
    private ClassSection classSection;
    
    /** 그룹 이름 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    /** 그룹 순서 인덱스 */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
    
    /** 그룹에 속한 사용자들 */
    @OneToMany(mappedBy = "studentGroup")
    private List<Account> users;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentGroup)) return false;
        StudentGroup that = (StudentGroup) o;
        return Objects.equals(studentGroupId, that.studentGroupId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(studentGroupId);
    }
}
