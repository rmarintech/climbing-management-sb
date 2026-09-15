package com.rubenmarin.climbingmanagementsb.dto;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;

import java.util.List;

public class CourseWithEnrollmentsDto {

    private String id;
    private String name;
    private Double price;
    private Difficulty difficulty;
    private List<EnrollmentDto> enrollments;

    public CourseWithEnrollmentsDto(
            String id,
            String name,
            Double price,
            Difficulty difficulty,
            List<EnrollmentDto> enrollments
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.difficulty = difficulty;
        this.enrollments = enrollments;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public List<EnrollmentDto> getEnrollments() {
        return enrollments;
    }

}