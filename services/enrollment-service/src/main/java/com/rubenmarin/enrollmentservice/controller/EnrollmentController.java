package com.rubenmarin.enrollmentservice.controller;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.adapter.in.rest.EnrollmentResponse;
import com.rubenmarin.enrollmentservice.model.Enrollment;
import com.rubenmarin.enrollmentservice.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    // Old architecture.
    // Temporarily kept for GET /enrollments.
    private final EnrollmentService enrollmentService;

    // New Hexagonal Architecture inbound port.
    private final CreateEnrollmentUseCase createEnrollmentUseCase;

    public EnrollmentController(EnrollmentService enrollmentService,
                                CreateEnrollmentUseCase createEnrollmentUseCase
    ) {
        this.enrollmentService = enrollmentService;
        this.createEnrollmentUseCase = createEnrollmentUseCase;
    }

    @GetMapping
    public List<Enrollment> findAll() {
        return enrollmentService.findAll();
    }

//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public Enrollment create(@RequestBody Enrollment enrollment) {
//        return enrollmentService.create(enrollment);
//    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse  create(@RequestBody Enrollment enrollment) {

        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        enrollment.courseId(),
                        enrollment.studentName()
                );

        com.rubenmarin.enrollmentservice.domain.model.Enrollment created =
                createEnrollmentUseCase.createEnrollment(command);

        return new EnrollmentResponse(
                created.getId().value(),
                created.getCourseId().value(),
                created.getStudentName().value(),
                created.getStatus().name()
        );
    }
}