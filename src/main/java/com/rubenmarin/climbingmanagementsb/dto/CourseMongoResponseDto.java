package com.rubenmarin.climbingmanagementsb.dto;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;

public class CourseMongoResponseDto {

    private String id;
    private String name;
    private Double price;
    private Difficulty difficulty;

    public CourseMongoResponseDto(String id, String name, Double price, Difficulty difficulty) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.difficulty = difficulty;
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

}
