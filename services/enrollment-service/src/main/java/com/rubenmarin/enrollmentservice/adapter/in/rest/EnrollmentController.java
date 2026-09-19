package com.rubenmarin.enrollmentservice.adapter.in.rest;

import com.rubenmarin.enrollmentservice.api.generated.api.EnrollmentsApi;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.application.port.in.FindEnrollmentsUseCase;

import com.rubenmarin.enrollmentservice.domain.model.Enrollment;

import com.rubenmarin.enrollmentservice.api.generated.model.EnrollmentRequest;
import com.rubenmarin.enrollmentservice.api.generated.model.EnrollmentResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EnrollmentController implements EnrollmentsApi {

    private final CreateEnrollmentUseCase createEnrollmentUseCase;
    private final FindEnrollmentsUseCase findEnrollmentsUseCase;

    public EnrollmentController(CreateEnrollmentUseCase createEnrollmentUseCase,
                                FindEnrollmentsUseCase findEnrollmentsUseCase
    ) {
        this.createEnrollmentUseCase = createEnrollmentUseCase;
        this.findEnrollmentsUseCase = findEnrollmentsUseCase;
    }

    @Override
    public ResponseEntity<EnrollmentResponse> createEnrollment(EnrollmentRequest enrollmentRequest) {

        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        enrollmentRequest.getCourseId(),
                        enrollmentRequest.getStudentName()
                );

        Enrollment created = createEnrollmentUseCase.createEnrollment(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toEnrollmentResponse(created));
    }

    @Override
    public ResponseEntity<List<EnrollmentResponse>> getEnrollments() {

        List<EnrollmentResponse> response = findEnrollmentsUseCase
                .findAll()
                .stream()
                .map(this::toEnrollmentResponse)
                .toList();

        return ResponseEntity.ok(response);
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