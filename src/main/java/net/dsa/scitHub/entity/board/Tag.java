package net.dsa.scitHub.entity.board;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"post"})
public class Tag {
    
    /** 태그 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Integer tagId;
    
    /** 태그가 달린 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    /** 태그 이름 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return Objects.equals(tagId, tag.tagId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }
}
