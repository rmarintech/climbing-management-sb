package com.rubenmarin.enrollmentservice.application.port.out;

import com.rubenmarin.enrollmentservice.domain.model.CourseId;

public interface CourseExistsPort {

    boolean existsById(CourseId courseId);
}