package net.dsa.scitHub.entity.classroom;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(
    name = "study_groups",
    uniqueConstraints = @UniqueConstraint(name = "uq_group_name", columnNames = {"cohort_no","class_section","name"})
)
public class StudyGroupsEntity {

    public enum ClassSection { A, B }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id", columnDefinition = "int unsigned")
    private Integer groupId;

    @Column(name = "cohort_no", nullable = false)
    private Integer cohortNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_section", nullable = false, length = 1)
    private ClassSection classSection;

    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "order_index", nullable = false, columnDefinition = "int default 0")
    private Integer orderIndex;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (orderIndex == null) orderIndex = 0;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // 그룹 과제
    @Builder.Default
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    private List<GroupAssignmentsEntity> assignments = new ArrayList<>();
}
