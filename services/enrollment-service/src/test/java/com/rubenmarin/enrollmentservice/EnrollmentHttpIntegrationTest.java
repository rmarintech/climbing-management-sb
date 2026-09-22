package com.rubenmarin.enrollmentservice;


import com.rubenmarin.enrollmentservice.adapter.out.course.rest.CourseRestAdapter;
import com.rubenmarin.enrollmentservice.document.EnrollmentDocument;
import com.rubenmarin.enrollmentservice.domain.model.CourseId;
import com.rubenmarin.enrollmentservice.domain.model.EnrollmentStatus;
import com.rubenmarin.enrollmentservice.repository.EnrollmentRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

//  EnrollmentControllerTest
//      → MVC slice
//      → application use cases mocked
//      → tests controller + security + validation
//
//  EnrollmentHttpIntegrationTest
//      → full Spring context
//      → application/service/persistence real
//      → real MongoDB
//      → only external systems mocked

// EnrollmentControllerTest:
//        MockMvc
//          ↓
//  real Controller
//          ↓
//  MOCK FindEnrollmentsUseCase
//      ✋ stops here

//EnrollmentHttpIntegrationTest:
//        MockMvc
//          ↓
//  real SecurityFilterChain
//          ↓
//  real EnrollmentController
//          ↓
//  real FindEnrollmentsService
//          ↓
//    FindEnrollmentsPort
//          ↓
//  real MongoEnrollmentAdapter
//          ↓
//  real EnrollmentRepository
//          ↓
//  REAL MongoDB Testcontainer
//          ✅
// only mock:
//        Keycloak/JWT decoder                          → external system
//        CourseRestAdapter / Course Service boundary   → external microservice

// GET persisted Enrollment → 200 + JSON                      ✅
// POST existing Course → 201 + persisted Mongo document      ✅
// POST missing Course → 404 + no Mongo persistence           ✅

@SpringBootTest(
        //deliberately a dummy value. It only satisfies configuration binding.
        properties = {
                "course-service.base-url=http://localhost:9999"
        }
)
@AutoConfigureMockMvc
@Testcontainers
//HTTP → Controller → Application → Persistence → MongoDB
class EnrollmentHttpIntegrationTest {
    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer =
            new MongoDBContainer("mongo:7.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // External authentication infrastructure is not part of this test.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // External Course Service is not part of this test either.
    // The real application service / Mongo adapter remain active.
    @MockitoBean
    private CourseRestAdapter courseRestAdapter;

    @BeforeEach
    void cleanDatabase() {
        enrollmentRepository.deleteAll();
    }
//    GET HTTP → Controller → Application → MongoDB → HTTP ✅
//    GET HTTP request
//          ↓
//    SecurityFilterChain
//          ↓
//    EnrollmentController
//          ↓
//    FindEnrollmentsService
//          ↓
//    MongoEnrollmentAdapter
//          ↓
//      MongoDB
//          ↓
//    MongoEnrollmentAdapter
//          ↓
//    FindEnrollmentsService
//          ↓
//    EnrollmentController
//          ↓
//    HTTP response (200 + JSON) ✅
    @Test
    void shouldReturnPersistedEnrollmentsThroughFullApplication() throws Exception {

        // Arrange: insert real data into the real MongoDB Testcontainer.
        EnrollmentDocument document =
                new EnrollmentDocument(
                        "http-integration-001",
                        10L,
                        "Rubén",
                        EnrollmentStatus.PENDING.name()
                );

        enrollmentRepository.save(document);

        // Act + Assert
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/enrollments")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt().authorities(
                                                new SimpleGrantedAuthority("ROLE_USER")
                                        )
                                )
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("http-integration-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].courseId").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].studentName").value("Rubén"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("PENDING"));
    }

//  POST HTTP → Security → Controller → Application → MongoDB → HTTP ✅
//  POST HTTP
//      ↓
//  Security
//      ↓
//  Controller
//      ↓
//  CreateEnrollmentService
//      ↓
//  CourseRestAdapter mock
//      ↓
//  MongoEnrollmentAdapter
//      ↓
//  MongoDB
//      ↓
//  Controller creates EnrollmentResponse
//      ↓
//  HTTP 201 Created + JSON ✅
    @Test
    void shouldCreateEnrollmentThroughFullApplication() throws Exception {

        // Arrange:
        // Simulate the external Course Service confirming that Course 10 exists.
        Mockito.when(courseRestAdapter.existsById(new CourseId(10L))).thenReturn(true);

        // Act + Assert:
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/enrollments")
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
                                          "studentName": "Rubén"
                                        }
                                        """)
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.courseId").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.studentName").value("Rubén"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("PENDING"));

        // Verify the external boundary was consulted.
        Mockito.verify(courseRestAdapter).existsById(new CourseId(10L));

        // Verify that the Enrollment really reached MongoDB.
        List<EnrollmentDocument> persisted = enrollmentRepository.findAll();

        Assertions.assertEquals(1, persisted.size());

        EnrollmentDocument saved = persisted.getFirst();

        Assertions.assertEquals(10L, saved.getCourseId());
        Assertions.assertEquals("Rubén", saved.getStudentName());
        Assertions.assertEquals(EnrollmentStatus.PENDING.name(), saved.getStatus());
    }


//    POST /enrollments
//        ↓
//    ROLE_ADMIN
//        ↓
//    real Controller
//        ↓
//    real CreateEnrollmentService
//        ↓
//    CourseRestAdapter mock → false
//            ↓
//    CourseNotFoundException
//        ↓
//    real GlobalExceptionHandler
//        ↓
//    404 Not Found
//        ↓
//    MongoDB remains empty
//        ✅

    @Test
    void shouldReturn404AndNotPersistWhenCourseDoesNotExist() throws Exception {

        // External Course Service says Course 999 does not exist.
        Mockito.when(courseRestAdapter.existsById(new CourseId(999L))).thenReturn(false);

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/enrollments")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                                )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "courseId": 999,
                                          "studentName": "Rubén"
                                        }
                                        """)
                )
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        Mockito.verify(courseRestAdapter).existsById(new CourseId(999L));

        Assertions.assertEquals(0, enrollmentRepository.count());
    }
}
