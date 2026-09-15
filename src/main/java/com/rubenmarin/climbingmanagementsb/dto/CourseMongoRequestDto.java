package com.rubenmarin.climbingmanagementsb.dto;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class CourseMongoRequestDto {

    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private Double price;

    @NotNull
    private Difficulty difficulty;

    public CourseMongoRequestDto() {
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

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
}
