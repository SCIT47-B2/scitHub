package net.dsa.scitHub.dto;

import java.util.List;
import java.util.OptionalDouble;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.course.Course;
import net.dsa.scitHub.entity.course.CourseReview;
import net.dsa.scitHub.enums.CourseType;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private Integer courseId;
    private String name;
    private CourseType courseType;
    private String instructorName;
    private Double averageRating;

    public static CourseDTO convertToCourseDTO(Course entity) {

        //CourseReview 목록에서 평균 평점 계산
        Double avgRating = 0.0;
        if (entity.getReviews() != null && !entity.getReviews().isEmpty()) {
            OptionalDouble optionalAvg = entity.getReviews().stream()
                .mapToDouble(CourseReview::getRating)
                .average();
            if (optionalAvg.isPresent()) {
                avgRating = Math.round(optionalAvg.getAsDouble() * 10) / 10.0; //소수점 첫째자리까지 반올림
            }
        }

        return CourseDTO.builder()
            .courseId(entity.getCourseId())
            .name(entity.getName())
            .courseType(entity.getCourseType())
            .instructorName(entity.getInstructorName())
            .averageRating(avgRating)
            .build();
    }


}
