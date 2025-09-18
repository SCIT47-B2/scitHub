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
    private String username;
    // private Integer scorePreparedness;
    // private Integer scoreProfesion;
    // private Integer scoreCommunication;
    // private Integer scoreEngagement;
    // private Integer scoreFairness;
    // private Integer courseDifficulty; 
    // private Integer courseAssignment;
    // private Integer courseConnectivity;
    private String commentText;
    private Byte rating;
 
    public static CourseReviewDTO convertToCourseReviewDTO(CourseReview entity) {
        return CourseReviewDTO.builder()
            .courseReviewId(entity.getCourseReviewId())
            .courseId(entity.getCourse().getCourseId())
            .userId(entity.getUser().getUserId())
            .username(entity.getUser().getUsername())
            .commentText(entity.getCommentText())
            .rating(entity.getRating())
            .build();
    }
    
}
