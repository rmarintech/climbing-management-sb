package com.rubenmarin.climbingmanagementsb.controller;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.exception.CourseNotFoundException;
import com.rubenmarin.climbingmanagementsb.exception.ExceptionMsg;
import com.rubenmarin.climbingmanagementsb.record.CourseRecord;
import com.rubenmarin.climbingmanagementsb.service.CourseServiceDto;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.util.List;


//"Quiero levantar la infraestructura MVC necesaria para probar CourseController, pero no toda la aplicación."
@WebMvcTest(CourseControllerDto.class)
public class CourseRecordEntityControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    //1. Mock
    @MockitoBean
    CourseServiceDto courseServiceDto;

    //En nuestros tests de Service hacíamos: when → assert → verify
    // Con MockMvc estamos haciendo: perform → andExpect → andExpect
        @Test
    void shouldFindAllCourses() throws Exception {

        // 1. Given / Stubbing
        List<CourseRecord> courses = List.of(
                new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY),
                new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM),
                new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD));

        // 2. When
        Mockito.when(courseServiceDto.findAll()).thenReturn(courses);

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.get("/dto/courses"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", Matchers.equalTo("Sport Climbing")));

        // 4. Verify
        Mockito.verify(courseServiceDto, Mockito.times(1)).findAll();
    }

    @Test
    void shouldFindCourse() throws Exception {

        // 1. Given / Stubbing
        List<CourseRecord> courses = List.of(
                new CourseRecord(1L, "Sport Climbing", 120.0, Difficulty.EASY),
                new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM),
                new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD));

        CourseRecord expectedCourse = new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM);

        // 2. When
        Mockito.when(courseServiceDto.findById(2L)).thenReturn(expectedCourse);

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.get("/dto/courses/2"))
                // .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.equalTo("Multi Pitch")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.difficulty", Matchers.equalTo(Difficulty.MEDIUM.name())));

        // 4. Verify
        Mockito.verify(courseServiceDto, Mockito.times(1)).findById(2L);
    }

    @Test
    void shouldReturn404WhenCourseDoesNotExist() throws Exception {

        // 2. When
        Mockito.when(courseServiceDto.findById(99L)).thenThrow(new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.get("/dto/courses/99"))
                //.andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo(HttpStatus.NOT_FOUND.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", Matchers.equalTo(ExceptionMsg.COURSE_NOT_FOUND)));

        // 4. Verify
        Mockito.verify(courseServiceDto, Mockito.times(1)).findById(99L);
    }

    @Test
    void shouldCreateCourse() throws Exception {
        CourseRecord course = new CourseRecord(null, "Trad Climbing", 150.0, Difficulty.HARD);
        CourseRecord savedCourse = new CourseRecord(4L, "Trad Climbing", 150.0, Difficulty.HARD);
        // 2. When
        Mockito.when(courseServiceDto.create(course)).thenReturn(savedCourse);

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.post("/dto/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(course)))
                // .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(4)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.equalTo("Trad Climbing")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.price", Matchers.equalTo(150.0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.difficulty", Matchers.equalTo("HARD")));

        // 4. Verify
        Mockito.verify(courseServiceDto).create(course);
    }

    @Test
    void shouldReturn400WhenCreatingInvalidCourse() throws Exception {
        String invalidCourse = """
                {
                    "name": "",
                    "price": -10,
                    "difficulty": null
                }
                """;

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.post("/dto/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCourse))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                // .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo(HttpStatus.BAD_REQUEST.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", Matchers.equalTo(ExceptionMsg.VALIDATION_FAILED)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.price", Matchers.equalTo(ExceptionMsg.MUST_BE_GREATER_0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.name", Matchers.equalTo(ExceptionMsg.MUST_NOT_BE_BLANK)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.difficulty", Matchers.equalTo(ExceptionMsg.MUST_NOT_BE_NULL)))
        ;

        // 4. Verify
        //  Mockito.verify(courseService).create(course);
    }

    @Test
    void shouldUpdateCourse() throws Exception {


        CourseRecord modifiedCourse = new CourseRecord(null, "Multi Pitch Upd", 135.0, Difficulty.HARD);
        CourseRecord updatedCourse = new CourseRecord(2L, "Multi Pitch Upd", 135.0, Difficulty.HARD);
        // 2. When
        Mockito.when(courseServiceDto.update(2L, modifiedCourse)).thenReturn(updatedCourse);

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.put("/dto/courses/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modifiedCourse)))
                //.andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.equalTo("Multi Pitch Upd")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.price", Matchers.equalTo(135.0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.difficulty", Matchers.equalTo("HARD")))

        ;

        // 4. Verify
        Mockito.verify(courseServiceDto).update(2L, modifiedCourse);
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistingCourse() throws Exception {
        CourseRecord modifiedCourse = new CourseRecord(null, "Multi Pitch Upd", 135.0, Difficulty.HARD);

        // 2. When
        Mockito.when(courseServiceDto.update(99L, modifiedCourse)).thenThrow(new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.put("/dto/courses/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modifiedCourse)))
                //.andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo(HttpStatus.NOT_FOUND.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", Matchers.equalTo(ExceptionMsg.COURSE_NOT_FOUND)));
        ;

        // 4. Verify
        Mockito.verify(courseServiceDto).update(99L, modifiedCourse);
    }

    @Test
    void shouldDeleteCourse() throws Exception {
        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.delete("/dto/courses/2"))
                //  .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNoContent())
        ;

        // 4. Verify
        Mockito.verify(courseServiceDto).delete(2L);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistingCourse() throws Exception {

        // 2. When
        Mockito.when(courseServiceDto.delete(99L)).thenThrow(new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.delete("/dto/courses/99"))
                //.andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.equalTo(HttpStatus.NOT_FOUND.value())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", Matchers.equalTo(ExceptionMsg.COURSE_NOT_FOUND)));
        ;

        // 4. Verify
        Mockito.verify(courseServiceDto).delete(99L);
    }

    @Test
    void shouldFindMostExpensiveCourse() throws Exception {
        // 1. Given
        CourseRecord mostExpensive = new CourseRecord(3L, "Alpine Climbing", 140.0, Difficulty.HARD);

        // 2. When
        Mockito.when(courseServiceDto.findMostExpensive()).thenReturn(mostExpensive);

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.get("/dto/courses/most-expensive"))
               // .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.equalTo(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.equalTo("Alpine Climbing")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.price", Matchers.equalTo(140.0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.difficulty", Matchers.equalTo(Difficulty.HARD.name())))
                 ;

        // 4. Verify
        Mockito.verify(courseServiceDto).findMostExpensive();
    }

    @Test
    void shouldFindCoursesByDifficulty() throws Exception {
        // 1. Given
           List<CourseRecord>   expectedCourses = List.of(
                new CourseRecord(2L, "Multi Pitch", 130.0, Difficulty.MEDIUM));


        // 2. When
        Mockito.when(courseServiceDto.findByDifficulty(Difficulty.MEDIUM)).thenReturn(expectedCourses);

        // 3. Perform
        mockMvc.perform(MockMvcRequestBuilders.get("/dto/courses/difficulty/MEDIUM"))
                // .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id", Matchers.equalTo(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name", Matchers.equalTo("Multi Pitch")))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].price", Matchers.equalTo(130.0)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].difficulty", Matchers.equalTo(Difficulty.MEDIUM.name())))
        ;

        // 4. Verify
        Mockito.verify(courseServiceDto).findByDifficulty(Difficulty.MEDIUM);
    }

}
