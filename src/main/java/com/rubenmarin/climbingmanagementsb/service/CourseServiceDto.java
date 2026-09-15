package com.rubenmarin.climbingmanagementsb.service;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.exception.CourseNotFoundException;
import com.rubenmarin.climbingmanagementsb.exception.ExceptionMsg;
import com.rubenmarin.climbingmanagementsb.record.CourseRecord;
import com.rubenmarin.climbingmanagementsb.repository.CourseRepositoryDto;
import org.springframework.stereotype.Service;


import java.util.Comparator;
import java.util.List;

@Service
public class CourseServiceDto {

    private final CourseRepositoryDto courseRepositoryDto;

    public CourseServiceDto(CourseRepositoryDto courseRepositoryDto) {
        this.courseRepositoryDto = courseRepositoryDto;
    }

    public List<CourseRecord> findAll() {
        return courseRepositoryDto.findAll();
    }

    public CourseRecord findById(Long id) {
        return courseRepositoryDto.findById(id).orElseThrow(() -> new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));
    }

    public CourseRecord create(CourseRecord course) {
        return courseRepositoryDto.save(course);
    }

    public CourseRecord update(Long id, CourseRecord course) {
        return courseRepositoryDto.update(id, course).orElseThrow(() -> new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));
    }

    public CourseRecord delete(Long id) {
        return courseRepositoryDto.delete(id).orElseThrow(() -> new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));
    }

    public CourseRecord findMostExpensive() {
        List<CourseRecord> courses = courseRepositoryDto.findAll();
        CourseRecord mostExpensive = courses.stream().max(Comparator.comparing(CourseRecord::price)).orElseThrow(() -> new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));
        return mostExpensive;
    }

    public List<CourseRecord> findByDifficulty(Difficulty difficulty) {
        List<CourseRecord> coursesByDif = courseRepositoryDto.findAll().stream().filter(course -> course.difficulty().equals(difficulty)).toList();
        return coursesByDif;
    }

}
