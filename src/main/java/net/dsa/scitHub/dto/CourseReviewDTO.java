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
    private Integer userCohortNo; // 작성자 기수
    private boolean isAuthor; // 현재 사용자가 작성자인지 여부
    private String commentText;
    private byte rating;

    public static CourseReviewDTO convertToCourseReviewDTO(CourseReview entity, Integer currentUserId) {
        CourseReviewDTO.CourseReviewDTOBuilder builder = CourseReviewDTO.builder()
            .courseReviewId(entity.getCourseReviewId())
            .courseId(entity.getCourse().getCourseId())
            .commentText(entity.getCommentText())
            .rating(entity.getRating());

        // 유저 정보 매핑
        if (entity.getUser() != null) {
            builder.userId(entity.getUser().getUserId())
                   .userCohortNo(entity.getUser().getCohortNo())
                   .isAuthor(entity.getUser().getUserId().equals(currentUserId)); // 작성자 여부 확인
        }

        return builder.build();
    }

}
