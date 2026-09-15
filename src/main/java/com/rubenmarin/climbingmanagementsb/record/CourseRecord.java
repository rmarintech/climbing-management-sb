package com.rubenmarin.climbingmanagementsb.record;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


public record CourseRecord(
        Long id,

        @NotBlank
        String name,

        @Positive
        Double price,

        @NotNull
        Difficulty difficulty
) {
}