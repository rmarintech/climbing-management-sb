package com.rubenmarin.enrollmentservice.client;

import com.rubenmarin.enrollmentservice.exception.CourseServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class CourseClient {

    private final RestClient restClient;

    public CourseClient(
            RestClient.Builder builder,
            @Value("${course-service.base-url}") String baseUrl) {


       //  connect timeout = 2s
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        // read timeout    = 2s
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
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
