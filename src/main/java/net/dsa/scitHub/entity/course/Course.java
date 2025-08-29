package net.dsa.scitHub.entity.course;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.enums.CourseType;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"reviews"})
public class Course {
    
    /** 강의 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;
    
    /** 강의 이름 */
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    
    /** 강의 유형 (IT, JP) */
    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false)
    private CourseType courseType;
    
    /** 기수 번호 */
    @Column(name = "cohort_no", nullable = false)
    private Integer cohortNo;
    
    /** 강사 이름 */
    @Column(name = "instructor_name", length = 50)
    private String instructorName;
    
    /** 강의에 대한 리뷰 목록 */
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<CourseReview> reviews;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return Objects.equals(courseId, course.courseId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(courseId);
    }
}
