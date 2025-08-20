package net.dsa.scitHub.entity.community;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "tags",
    uniqueConstraints = @UniqueConstraint(name = "uk_tags_name", columnNames = "name")
)
public class TagsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", columnDefinition = "int unsigned")
    private Integer tagId;

    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY)
    private List<PostTagsEntity> postTags = new ArrayList<>();
}
