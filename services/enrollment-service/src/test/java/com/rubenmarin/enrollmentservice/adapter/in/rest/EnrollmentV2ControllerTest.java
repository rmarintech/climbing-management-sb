package com.rubenmarin.enrollmentservice.adapter.in.rest;

import com.rubenmarin.enrollmentservice.api.generated.api.EnrollmentsV2Api;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.application.port.in.FindEnrollmentsUseCase;
import com.rubenmarin.enrollmentservice.configuration.SecurityConfiguration;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentId;
import com.rubenmarin.enrollmentservice.domain.model.StudentName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

// @MockitoBean :  Create Mockito mock --> register it as Spring bean --> inject it into EnrollmentController
// Pure Mockito unit test → @Mock + @InjectMocks
// Spring test → @MockitoBean for mocked Spring dependencies

@WebMvcTest(EnrollmentV2Controller.class)
@Import(SecurityConfiguration.class)
class EnrollmentV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateEnrollmentUseCase createEnrollmentUseCase;

    @MockitoBean
    private FindEnrollmentsUseCase findEnrollmentsUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;


    @Test
    void shouldReturn401WhenGetEnrollmentsWithoutAuthentication() throws Exception {
        // The request should be rejected before the controller/use case is reached.
        mockMvc.perform(
                        MockMvcRequestBuilders.get(EnrollmentsV2Api.PATH_GET_ENROLLMENTS_V2)
                )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        Mockito.verify(findEnrollmentsUseCase, Mockito.never()).findAll();
    }

    @Test
    void shouldReturnEnrollmentsForAuthenticatedUser() throws Exception {

        // Arrange
        Enrollment enrollment = new Enrollment(
                new EnrollmentId("integration-001"),
                new CourseId(10L),
                new StudentName("Rubén")
        );

        Mockito.when(findEnrollmentsUseCase.findAll()).thenReturn(List.of(enrollment));

        // Act + Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.get(EnrollmentsV2Api.PATH_GET_ENROLLMENTS_V2)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(new SimpleGrantedAuthority("ROLE_USER")
                                                )
                                )
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("integration-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].courseId").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].studentName").value("Rubén"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("PENDING"))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Deprecation"));

        Mockito.verify(findEnrollmentsUseCase).findAll();
    }


    @Test
    void shouldReturnEnrollmentsForAuthenticatedAdmin() throws Exception {

        // Arrange
        Enrollment enrollment = new Enrollment(
                new EnrollmentId("integration-001"),
                new CourseId(10L),
                new StudentName("Rubén")
        );

        Mockito.when(findEnrollmentsUseCase.findAll()).thenReturn(List.of(enrollment));

        // Act + Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.get(EnrollmentsV2Api.PATH_GET_ENROLLMENTS_V2)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")
                                                )
                                )
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("integration-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].courseId").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].studentName").value("Rubén"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("PENDING"))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Deprecation"));


        Mockito.verify(findEnrollmentsUseCase).findAll();
    }

    @Test
    void shouldReturn401WhenCreatingEnrollmentWithoutAuthentication() throws Exception {

        mockMvc.perform(
                        MockMvcRequestBuilders.post(EnrollmentsV2Api.PATH_CREATE_ENROLLMENT_V2)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": 10,
                                          "student": {
                                              "name": "Rubén"
                                            }
                                        }
                                        """)
                )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        Mockito.verify(createEnrollmentUseCase, Mockito.never())
                .createEnrollment(Mockito.any());
    }


    @Test
    void shouldReturn403WhenCreatingEnrollmentWithAuthenticatedUser() throws Exception {

        mockMvc.perform(
                        MockMvcRequestBuilders.post(EnrollmentsV2Api.PATH_CREATE_ENROLLMENT_V2)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(new SimpleGrantedAuthority("ROLE_USER")
                                                )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": 10,
                                          "student": {
                                              "name": "Rubén"
                                            }
                                        }
                                        """)
                )
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        Mockito.verify(createEnrollmentUseCase, Mockito.never())
                .createEnrollment(Mockito.any());
    }

    @Test
    void shouldReturn201WhenCreatingEnrollmentWithAuthenticatedAdmin() throws Exception {


        // Arrange
        Enrollment enrollment = new Enrollment(
                new EnrollmentId("integration-001"),
                new CourseId(10L),
                new StudentName("Rubén")
        );


        Mockito.when(createEnrollmentUseCase.createEnrollment(
                                Mockito.any()
                        )
                )
                .thenReturn(enrollment);


        mockMvc.perform(
                        MockMvcRequestBuilders.post(EnrollmentsV2Api.PATH_CREATE_ENROLLMENT_V2)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")
                                                )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": 10,
                                           "student": {
                                              "name": "Rubén"
                                            }
                                        }
                                        """)
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("integration-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.courseId").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.studentName").value("Rubén"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("PENDING"))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Deprecation"));

        //  prove that the controller correctly converted
        // V2 JSON {"courseId":10,"student":{"name":"Rubén"}}
        // → CreateEnrollmentCommand(10L, "Rubén")

        // ArgumentCaptor lets Mockito capture the actual argument that
        // was passed into a mocked method, so you can inspect it afterward.
        ArgumentCaptor<CreateEnrollmentCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateEnrollmentCommand.class);

        Mockito.verify(createEnrollmentUseCase).createEnrollment(commandCaptor.capture());

        CreateEnrollmentCommand createEnrollmentCommand = commandCaptor.getValue();

        Assertions.assertEquals(10L, createEnrollmentCommand.courseId());
        Assertions.assertEquals("Rubén", createEnrollmentCommand.studentName());
    }

    @Test
    void shouldReturn400WhenCourseIdIsInvalid() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post(EnrollmentsV2Api.PATH_CREATE_ENROLLMENT_V2)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                                )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": 0,
                                          "student": {
                                              "name": "Rubén"
                                            }
                                        }
                                        """)
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        Mockito.verify(createEnrollmentUseCase, Mockito.never())
                .createEnrollment(Mockito.any());
    }

    @Test
    void shouldReturn400WhenStudentNameIsBlank() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post(EnrollmentsV2Api.PATH_CREATE_ENROLLMENT_V2)
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                                )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": 10,
                                          "student": {
                                              "name": ""
                                            }
                                        }
                                        """)
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        Mockito.verify(createEnrollmentUseCase, Mockito.never())
                .createEnrollment(Mockito.any());
    }


    //OLD API is not called
    @Test
    void shouldReturn404ForOldUnversionedEndpoint() throws Exception {

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/enrollments")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority("ROLE_USER")
                                                )
                                )
                )
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        Mockito.verify(findEnrollmentsUseCase, Mockito.never()).findAll();
    }
}
