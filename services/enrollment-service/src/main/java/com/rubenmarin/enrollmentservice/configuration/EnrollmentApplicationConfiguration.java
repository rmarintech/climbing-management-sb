package com.rubenmarin.enrollmentservice.configuration;

import com.rubenmarin.enrollmentservice.application.port.in.CreateEnrollmentUseCase;
import com.rubenmarin.enrollmentservice.application.port.out.CourseExistsPort;
import com.rubenmarin.enrollmentservice.application.port.out.SaveEnrollmentPort;
import com.rubenmarin.enrollmentservice.application.service.CreateEnrollmentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnrollmentApplicationConfiguration {

    @Bean
//    Spring now sees these two beans because our adapters have @Component

//    Our current approach gives us:
//     - domain   → pure Java
//     - application    → pure Java
//     - adapters/configuration   → Spring


//    Dependency inversion gave us:
//
//    CreateEnrollmentService
//        ↓
//    CourseExistsPort
//    SaveEnrollmentPort

//    Dependency injection is Spring actually supplying the implementations:
//
//  CourseExistsPort
//          ↑
//  CourseRestAdapter
//
//   SaveEnrollmentPort
//          ↑
//  MongoEnrollmentAdapter

    public CreateEnrollmentUseCase createEnrollmentUseCase(
            CourseExistsPort courseExistsPort,
            SaveEnrollmentPort saveEnrollmentPort
    ) {

        return new CreateEnrollmentService(courseExistsPort, saveEnrollmentPort);
    }
}