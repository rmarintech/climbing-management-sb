package com.rubenmarin.climbingmanagementsb.document;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;


/*
 * COMPOUND INDEX
 *
 * Indexes both difficulty and price because our queries frequently filter by difficulty and then by price.
 *
 * Field order matters: difficulty is the leading field, followed by price.
 *
 * 1 → ascending order.
 */
@CompoundIndex(
        name = "difficulty_price_idx",
        def = "{'difficulty': 1, 'price': 1}"
)

@Document(collection = "courses")
public class CourseMongoDocument {

    @Id
    private String id;

    private String name;

    private Double price;

   // @Indexed
    private Difficulty difficulty;

    protected CourseMongoDocument() {
    }

    public CourseMongoDocument(String name, Double price, Difficulty difficulty) {
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
