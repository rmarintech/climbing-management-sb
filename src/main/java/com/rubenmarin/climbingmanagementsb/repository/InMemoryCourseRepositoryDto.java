package com.rubenmarin.climbingmanagementsb.repository;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.record.CourseRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public class InMemoryCourseRepositoryDto implements CourseRepositoryDto {

    private final List<CourseRecord> courses = new ArrayList<CourseRecord>();

    public InMemoryCourseRepositoryDto() {
        courses.add(new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY));
        courses.add(new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM));
        courses.add(new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD));
    }

    @Override
    public List<CourseRecord> findAll() {
        return courses;
    }

    @Override
    public Optional<CourseRecord> findById(Long id) {
        Stream<CourseRecord> st = this.findAll().stream().filter(course -> course.id().equals(id));
        Optional<CourseRecord> result = st.findFirst();
        return result;
    }

    @Override
    public CourseRecord save(CourseRecord course) {
        Long lastId = courses.stream().max(Comparator.comparing(CourseRecord::id)).map(CourseRecord::id).orElse(0L);
        CourseRecord newCourse = new CourseRecord(lastId + 1L, course.name(), course.price(), course.difficulty());
        courses.add(newCourse);
        return newCourse;
    }

    @Override
    public Optional<CourseRecord> update(Long id, CourseRecord course) {
        Optional<CourseRecord> found = this.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        CourseRecord newCourse = new CourseRecord(id, course.name(), course.price(), course.difficulty());
        int posicion = -1;
        List<CourseRecord> allCourses = this.findAll();
        for (int i = 0; i < allCourses.size(); i++) {
            if (allCourses.get(i).id().equals(id)) {
                posicion = i;
                break;
            }
        }
        allCourses.set(posicion, newCourse);
        return Optional.of(newCourse);
    }


    @Override
    public Optional<CourseRecord> delete(Long id) {
        Optional<CourseRecord> found = this.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        this.findAll().remove(found.get());
        return found;
    }
}
