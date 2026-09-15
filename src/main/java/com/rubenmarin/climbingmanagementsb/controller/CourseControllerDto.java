package com.rubenmarin.climbingmanagementsb.controller;

import com.rubenmarin.climbingmanagementsb.record.CourseRecord;
import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.service.CourseServiceDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dto/courses")
public class CourseControllerDto {

    private final CourseServiceDto courseService;

    public CourseControllerDto(CourseServiceDto courseService) {
        this.courseService = courseService;
    }

     @GetMapping()
    public List<CourseRecord> getCourses() {
        return courseService.findAll();
    }

    @GetMapping("/{id}")
    public CourseRecord getCourseById(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @PostMapping()
    public ResponseEntity<CourseRecord> createCourse(@Valid @RequestBody CourseRecord course) {
        CourseRecord created = courseService.create(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseRecord> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRecord course) {
        CourseRecord updated = courseService.update(id, course);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CourseRecord> deleteCourse(@PathVariable Long id) {
        CourseRecord deleted = courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/most-expensive")
    public ResponseEntity<CourseRecord> findMostExpensiveCourse() {
        CourseRecord mostExpensive = courseService.findMostExpensive();
        return ResponseEntity.status(HttpStatus.OK).body(mostExpensive);
    }

    @GetMapping("/difficulty/{difficulty}")
    public List<CourseRecord> findByDifficulty(@PathVariable Difficulty difficulty) {
        return courseService.findByDifficulty(difficulty);
    }

}
