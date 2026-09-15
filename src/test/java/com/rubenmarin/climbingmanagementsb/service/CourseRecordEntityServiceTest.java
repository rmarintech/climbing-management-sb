
package com.rubenmarin.climbingmanagementsb.service;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.exception.CourseNotFoundException;
import com.rubenmarin.climbingmanagementsb.exception.ExceptionMsg;
import com.rubenmarin.climbingmanagementsb.record.CourseRecord;
import com.rubenmarin.climbingmanagementsb.repository.CourseRepositoryDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CourseRecordEntityServiceTest {
    //1. Mock
    @Mock
    CourseRepositoryDto courseRepositoryDto;
    @InjectMocks
    CourseServiceDto courseServiceDto;

    @Test
    void shouldFindCourseById() {
        CourseRecord course = new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY);
        //2. Stubbing
        Mockito.when(courseRepositoryDto.findById(1L)).thenReturn(Optional.of(course));
        //3. Assertion
        Assertions.assertEquals(course, courseServiceDto.findById(1L));
    }

    @Test
    void shouldReturnNullWhenCourseDoesNotExist() {
        //2. Stubbing
        Mockito.when(courseRepositoryDto.findById(99L)).thenReturn(Optional.empty());
        //3. Assertions
        CourseNotFoundException ex = Assertions.assertThrows(CourseNotFoundException.class, () -> courseServiceDto.findById(99L));
        Assertions.assertEquals(ExceptionMsg.COURSE_NOT_FOUND, ex.getMessage());
    }

    @Test
    void shouldCreateCourse() {
        CourseRecord course = new CourseRecord(null, "Trad Climbing", 150.0, Difficulty.MEDIUM);
        CourseRecord savedCourse = new CourseRecord(4L, "Trad Climbing", 150.0, Difficulty.MEDIUM);

        //2. Stubbing
        Mockito.when(courseRepositoryDto.save(course)).thenReturn(savedCourse);

        //3. Assertions
        Assertions.assertEquals(savedCourse, courseServiceDto.create(course));

        //4. Verifications
        Mockito.verify(courseRepositoryDto).save(course);
    }

    @Test
    void shouldUpdateCourse() {
        CourseRecord course = new CourseRecord(null, "Multi Pitch", 160.0, Difficulty.HARD);
        CourseRecord updatedCourse = new CourseRecord(2L, "Multi Pitch Updated", 160.0, Difficulty.HARD);

        //2. Stubbing
        Mockito.when(courseRepositoryDto.update(2L, course)).thenReturn(Optional.of(updatedCourse));

        //3. Assertions
        Assertions.assertEquals(updatedCourse, courseServiceDto.update(2L, course));

        //4. Verifications
        Mockito.verify(courseRepositoryDto).update(2L, course);
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingNonExistingCourse() {
        CourseRecord course = new CourseRecord(null, "Sport Climbing", 120.0, Difficulty.EASY);

        //2. Stubbing
        Mockito.when(courseRepositoryDto.update(99L, course)).thenReturn(Optional.empty());

        //3. Assertion
        CourseNotFoundException ex = Assertions.assertThrows(CourseNotFoundException.class, () -> courseServiceDto.update(99L, course));
        Assertions.assertEquals(ExceptionMsg.COURSE_NOT_FOUND, ex.getMessage());

        //4. Verifications
        Mockito.verify(courseRepositoryDto).update(99L, course);
    }

    @Test
    void shouldDeleteCourse() {
        CourseRecord course = new CourseRecord(2L, "Sport Climbing", 120.0, Difficulty.EASY);

        //2. Stubbing
        Mockito.when(courseRepositoryDto.delete(2L)).thenReturn(Optional.of(course));

        //3. Assertions
        Assertions.assertEquals(course, courseServiceDto.delete(2L));

        //4. Verifications
        Mockito.verify(courseRepositoryDto).delete(2L);

    }

    @Test
    void shouldThrowNotFoundWhenDeletingNonExistingCourse() {
        CourseRecord course = new CourseRecord(2L, "Sport Climbing", 120.0, Difficulty.EASY);

        //2. Stubbing
        Mockito.when(courseRepositoryDto.delete(99L)).thenReturn(Optional.empty());

        //3. Assertions
        CourseNotFoundException ex = Assertions.assertThrows(CourseNotFoundException.class, () -> courseServiceDto.delete(99L));
        Assertions.assertEquals(ExceptionMsg.COURSE_NOT_FOUND, ex.getMessage());

        //4. Verifications
        Mockito.verify(courseRepositoryDto).delete(99L);

    }

    @Test
    void shouldFindAllCourses() {
        List<CourseRecord> courses = List.of(
                new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY),
                new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM),
                new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD));

        //2. Stubbing
        Mockito.when(courseRepositoryDto.findAll()).thenReturn(courses);

        //3. Assertions
        Assertions.assertEquals(courses, courseRepositoryDto.findAll());

        //4. Verifications
        Mockito.verify(courseRepositoryDto).findAll();
    }

    @Test
    void findMostExpensive() {
        List<CourseRecord> courses = List.of(
                new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY),
                new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM),
                new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD));

        CourseRecord mostExpensive = new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD);

        //2. Stubbing
        //La clase que estamos testeando no se mockea. !!!
        // Mockito.when(courseService.findMostExpensive()).thenReturn(mostExpensive);
        Mockito.when(courseRepositoryDto.findAll()).thenReturn(courses);

        //3. Assertions
        Assertions.assertEquals(mostExpensive, courseServiceDto.findMostExpensive());

        //4. Verifications
        Mockito.verify(courseRepositoryDto).findAll();
    }

    @Test
    void findMostExpensiveShouldThrowNotFoundWhenThereAreNoCourses() {
        //2. Stubbing
        Mockito.when(courseRepositoryDto.findAll()).thenReturn(List.of());

        //3. Assertions
        CourseNotFoundException ex = Assertions.assertThrows(CourseNotFoundException.class, () -> courseServiceDto.findMostExpensive());
        Assertions.assertEquals(ExceptionMsg.COURSE_NOT_FOUND, ex.getMessage());

        //4. Verifications
        Mockito.verify(courseRepositoryDto).findAll();
    }

    @Test
    void shouldFindCoursesByDifficulty() {

        List<CourseRecord> courses = List.of(
                new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY),
                new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM),
                new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD));

        CourseRecord expectedCourse = new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY);

        //2. Stubbing
        Mockito.when(courseRepositoryDto.findAll()).thenReturn(courses);

        //3. Assertions
        List<CourseRecord> result = courseServiceDto.findByDifficulty(Difficulty.EASY);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expectedCourse, result.getFirst());

        //4. Verifications
        Mockito.verify(courseRepositoryDto).findAll();
    }
}

//()


