package net.dsa.scitHub.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import groovy.util.logging.Slf4j;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.dto.CourseDTO;
import net.dsa.scitHub.entity.course.Course;
import net.dsa.scitHub.enums.CourseType;
import net.dsa.scitHub.repository.course.CourseRepository;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CourseService {

    @Autowired
    private CourseRepository cr;
    
    /**
     * 강의 목록을 이름으로 검색하거나 전체를 조회하는 메서드
     * @param name 검색할 강의명 (null이거나 비어있으면 전체 조회)
     * @return CourseDTO 리스트
     */
    public List<CourseDTO> getCourseList(String name) {
        List<Course> courseList;

        if (name != null && !name.trim().isEmpty()) {
            // 검색어가 있는 경우: 이름으로 검색
            courseList = cr.findByNameContaining(name);
        } else {
            // 검색어가 없는 경우: 전체 강의 조회
            courseList = cr.findAll();
        }
        
        return courseList.stream()
                .map(CourseDTO::convertToCourseDTO)
                .collect(Collectors.toList());
    }

    /**
     * ID로 강의 조회
     * @param courseId
     * @return
     */
    public CourseDTO selectById(Integer courseId) {
        Course course = cr.findById(courseId).orElse(null);
        if (course != null) {
            return CourseDTO.convertToCourseDTO(course);
        } else {
            return null;
        }
    }

}
