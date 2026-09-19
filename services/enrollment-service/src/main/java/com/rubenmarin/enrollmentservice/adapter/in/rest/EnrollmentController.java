package com.rubenmarin.enrollmentservice.adapter.in.rest;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.application.port.in.FindEnrollmentsUseCase;

import com.rubenmarin.enrollmentservice.domain.model.Enrollment;

import com.rubenmarin.enrollmentservice.api.generated.model.EnrollmentRequest;
import com.rubenmarin.enrollmentservice.api.generated.model.EnrollmentResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {


    // New Hexagonal Architecture inbound port.
    private final CreateEnrollmentUseCase createEnrollmentUseCase;
    private final FindEnrollmentsUseCase findEnrollmentsUseCase;

    public EnrollmentController(CreateEnrollmentUseCase createEnrollmentUseCase,
                                FindEnrollmentsUseCase findEnrollmentsUseCase
    ) {
        this.createEnrollmentUseCase = createEnrollmentUseCase;
        this.findEnrollmentsUseCase = findEnrollmentsUseCase;
    }


    @GetMapping
    public List<EnrollmentResponse> findAll() {
        return findEnrollmentsUseCase
                .findAll()
                .stream()
                .map(this::toEnrollmentResponse)
                .toList();
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse create(@Valid @RequestBody EnrollmentRequest enrollmentRequest) {

        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        enrollmentRequest.getCourseId(),
                        enrollmentRequest.getStudentName()
                );

        Enrollment created = createEnrollmentUseCase.createEnrollment(command);

        return toEnrollmentResponse(created);

    }


    private EnrollmentResponse toEnrollmentResponse(Enrollment enrollment) {

        return new EnrollmentResponse(
                enrollment.getId().value(),
                enrollment.getCourseId().value(),
                enrollment.getStudentName().value(),
                EnrollmentResponse.StatusEnum.fromValue(
                        enrollment.getStatus().name()
                )
        );
    }


}