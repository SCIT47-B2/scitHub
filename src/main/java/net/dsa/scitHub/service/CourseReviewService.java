package net.dsa.scitHub.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import groovy.util.logging.Slf4j;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.dto.CourseReviewDTO;
import net.dsa.scitHub.entity.course.Course;
import net.dsa.scitHub.entity.course.CourseReview;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.course.CourseRepository;
import net.dsa.scitHub.repository.course.CourseReviewRepository;
import net.dsa.scitHub.repository.user.UserRepository;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CourseReviewService {

    @Autowired
    private CourseRepository cr;
    @Autowired
    private CourseReviewRepository crr;
    private final UserRepository ur;


    /**
     * 강의 ID로 강의 리뷰 조회
     * @param courseId
     * @return
     */
    public List<CourseReviewDTO> selectByCourseId(Integer courseId) {
        return crr.findByCourse_CourseId(courseId).stream()
                .map(CourseReviewDTO::convertToCourseReviewDTO)
                .toList();
    }

    /**
     * 리뷰 등록 처리
     * @param courseId
     * @param username
     * @param reviewDTO
     */
    @Transactional
	public void createReview(Integer courseId, String username, CourseReviewDTO reviewDTO) {
		// 강의 정보 조회
        Course course = cr.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("該当コースが見つかりません. id=" + courseId));

        // 사용자 정보 조회
        User user = ur.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません. username=" + username));

        // DTO를 Entity로 변환
        CourseReview review = CourseReview.builder()
            .course(course)
            .user(user)
            .rating(reviewDTO.getRating())
            .commentText(reviewDTO.getCommentText())
            .build();

        // 리뷰 저장
        crr.save(review);
	}
}
