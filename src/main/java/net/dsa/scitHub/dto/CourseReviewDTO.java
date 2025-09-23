package net.dsa.scitHub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.course.CourseReview;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewDTO {

    private Integer courseReviewId;
    private Integer courseId;
    private Integer userId;
    private String username; // 작성자 ID
    private Integer userCohortNo; // 작성자 기수
    private String commentText;
    private Byte rating;
 
    public static CourseReviewDTO convertToCourseReviewDTO(CourseReview entity) {
        CourseReviewDTO.CourseReviewDTOBuilder builder = CourseReviewDTO.builder()
            .courseReviewId(entity.getCourseReviewId())
            .courseId(entity.getCourse().getCourseId())
            .commentText(entity.getCommentText())
            .rating(entity.getRating());
        
        // 유저 정보 매핑
        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getUserId())
                   .username(entity.getUser().getUsername())
                   .userCohortNo(entity.getUser().getCohortNo());
        }
        
        return builder.build();
    }
    
}
