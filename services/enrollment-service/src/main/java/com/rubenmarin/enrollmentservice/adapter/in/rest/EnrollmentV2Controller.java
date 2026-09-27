package com.rubenmarin.enrollmentservice.adapter.in.rest;

import com.rubenmarin.enrollmentservice.api.generated.api.EnrollmentsV2Api;
import com.rubenmarin.enrollmentservice.api.generated.model.EnrollmentResponse;
import com.rubenmarin.enrollmentservice.api.generated.model.EnrollmentV2Request;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentCommand;
import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.application.port.in.FindEnrollmentsUseCase;
import com.rubenmarin.enrollmentservice.domain.model.Enrollment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EnrollmentV2Controller implements EnrollmentsV2Api {

    private final CreateEnrollmentUseCase createEnrollmentUseCase;
    private final FindEnrollmentsUseCase findEnrollmentsUseCase;

    public EnrollmentV2Controller(CreateEnrollmentUseCase createEnrollmentUseCase,
                                  FindEnrollmentsUseCase findEnrollmentsUseCase
    ) {
        this.createEnrollmentUseCase = createEnrollmentUseCase;
        this.findEnrollmentsUseCase = findEnrollmentsUseCase;
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

    @Override
    public ResponseEntity<EnrollmentResponse> createEnrollmentV2(EnrollmentV2Request enrollmentV2Request) {
        CreateEnrollmentCommand command =
                new CreateEnrollmentCommand(
                        enrollmentV2Request.getCourseId(),
                        enrollmentV2Request.getStudent().getName()
                );

        Enrollment created = createEnrollmentUseCase.createEnrollment(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toEnrollmentResponse(created));
    }

    @Override
    public ResponseEntity<List<EnrollmentResponse>> getEnrollmentsV2() {
        List<EnrollmentResponse> response = findEnrollmentsUseCase
                .findAll()
                .stream()
                .map(this::toEnrollmentResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}