package com.rubenmarin.climbingmanagementsb.entity;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class CourseEntity {

    /*
     * @Id identifies the primary key of the entity.
     *
     * @GeneratedValue tells JPA that the database is responsible
     * for generating the ID.
     *
     * IDENTITY uses the database identity/auto-increment mechanism.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * OPTIMISTIC LOCKING
     *
     * Hibernate uses this field to detect concurrent modifications.
     *
     * When an entity is updated, Hibernate includes the current version
     * in the WHERE clause:
     *
     * UPDATE courses
     * SET ...
     *     version = ?
     * WHERE id = ?
     *   AND version = ?
     *
     * If another transaction has already changed the entity,
     * the version will no longer match and the update affects 0 rows.
     *
     * Hibernate then throws an optimistic locking exception.
     *
     * The version is automatically incremented by Hibernate.
     */
    @Version
    private Long version;


    private String name;

    private Double price;


    /*
     * Stores the enum as its String name in the database:
     *
     * EASY
     * MEDIUM
     * HARD
     *
     * Using STRING is safer than ORDINAL because the database does not
     * depend on the numeric position of the enum values.
     */
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;


    /*
     * JPA requires a no-argument constructor.
     *
     * protected prevents normal application code from using this
     * constructor while still allowing JPA/Hibernate to instantiate
     * the entity.
     */
    protected CourseEntity() {
    }


    public CourseEntity(String name, Double price, Difficulty difficulty) {
        this.name = name;
        this.price = price;
        this.difficulty = difficulty;
    }


    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
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


    /*
     * Entities are mutable because Hibernate tracks changes made to
     * managed entities through the Persistence Context.
     *
     * This mechanism is called Dirty Checking.
     */
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