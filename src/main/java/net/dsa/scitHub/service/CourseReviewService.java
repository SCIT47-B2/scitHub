package net.dsa.scitHub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import groovy.util.logging.Slf4j;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.repository.course.CourseReviewRepository;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CourseReviewService {

    @Autowired
    private CourseReviewRepository crr;
}
