package com.rubenmarin.enrollmentservice.client;

import com.rubenmarin.enrollmentservice.exception.CourseServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class CourseClient {

    private final RestClient restClient;

    public CourseClient(
            RestClient.Builder builder,
            @Value("${course-service.base-url}") String baseUrl) {

        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    public boolean courseExists(Long courseId) {
        try {
            return restClient.get()
                    .uri("/jpa/courses/{id}", courseId)
                    .exchange((request, response) -> {

                        if (response.getStatusCode().is2xxSuccessful()) {
                            return true;
                        }

                        if (response.getStatusCode().value() == 404) {
                            return false;
                        }

                        throw new IllegalStateException(
                                "Unexpected response from Course Service: "
                                        + response.getStatusCode()
                        );
                    });
        } catch (ResourceAccessException exception) {
            throw new CourseServiceUnavailableException(exception);
        }
    }
}
