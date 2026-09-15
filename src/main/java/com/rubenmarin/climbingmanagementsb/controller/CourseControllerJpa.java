package com.rubenmarin.climbingmanagementsb.controller;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.record.CourseRecord;
import com.rubenmarin.climbingmanagementsb.service.CourseServiceJpa;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jpa/courses")
public class CourseControllerJpa {

    private final CourseServiceJpa courseServiceJpa;



    public CourseControllerJpa(CourseServiceJpa courseServiceJpa) {
        this.courseServiceJpa = courseServiceJpa;
    }


    /*
     * GET /jpa/courses
     *
     * Returns all courses.
     */
    @GetMapping
    public List<CourseRecord> getCourses() {
        return courseServiceJpa.findAll();
    }


    /*
     * GET /jpa/courses/most-expensive
     *
     * Returns the course with the highest price.
     *
     * Specific endpoints are declared before /{id} for clarity.
     */
    @GetMapping("/most-expensive")
    public ResponseEntity<CourseRecord> findMostExpensiveCourse() {
        CourseRecord mostExpensive = courseServiceJpa.findMostExpensive();
        return ResponseEntity.ok(mostExpensive);
    }


    /*
     * GET /jpa/courses/difficulty/{difficulty}
     *
     * Returns all courses matching the specified difficulty.
     */
    @GetMapping("/difficulty/{difficulty}")
    public List<CourseRecord> findByDifficulty(@PathVariable Difficulty difficulty) {

        return courseServiceJpa.findByDifficulty(difficulty);
    }


    /*
     * GET /jpa/courses/difficulty/{difficulty}/price/{price}
     *
     * Returns courses with the specified difficulty and a price lower than the given value.
     */
    @GetMapping("/difficulty/{difficulty}/price/{price}")
    public List<CourseRecord> findByDifficultyAndPriceLessThan(@PathVariable Difficulty difficulty, @PathVariable Double price) {
        return courseServiceJpa.findByDifficultyAndPriceLessThan(difficulty, price);
    }


    /*
     * GET /jpa/courses/{id}
     *
     * Returns a single course by ID.
     */
    @GetMapping("/{id}")
    public CourseRecord getCourseById(@PathVariable Long id) {
        return courseServiceJpa.findById(id);
    }


    /*
     * POST /jpa/courses
     *
     * @Valid triggers Bean Validation on the incoming request body.
     *
     * HTTP 201 CREATED is returned when the course is successfully created.
     */
    @PostMapping
    public ResponseEntity<CourseRecord> createCourse(@Valid @RequestBody CourseRecord courseRecord) {
        CourseRecord created = courseServiceJpa.create(courseRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    /*
     * PUT /jpa/courses/{id}
     *
     * Updates an existing course.
     *
     * The transaction itself is handled by the service layer,not by the controller.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CourseRecord> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRecord courseRecord) {
        CourseRecord updated = courseServiceJpa.update(id, courseRecord);
        return ResponseEntity.ok(updated);
    }


    /*
     * DELETE /jpa/courses/{id}
     *
     * Deletes an existing course.
     *
     * HTTP 204 NO CONTENT indicates successful deletion without returning a response body.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseServiceJpa.delete(id);
        return ResponseEntity.noContent().build();
    }


    /*
     * TRANSACTION TEST ENDPOINT
     *
     * Used to demonstrate transaction rollback with REQUIRED.
     *
     * Both updates are performed inside the same transaction.
     * The RuntimeException causes the transaction to roll back.
     */
    @GetMapping("/test-required/{id1}/{id2}")
    public ResponseEntity<Void> testRequired(@PathVariable Long id1, @PathVariable Long id2) {

        courseServiceJpa.testRequired(id1, id2);
        return ResponseEntity.ok().build();
    }


    /*
     * TRANSACTION PROPAGATION TEST
     *
     * Demonstrates REQUIRED vs REQUIRES_NEW.
     *
     * operationA() starts the outer transaction.
     * operationB() uses REQUIRES_NEW and therefore runs in an independent transaction.
     */
    @GetMapping("/test-requires-new/{id}")
    public ResponseEntity<Void> testRequiresNew(@PathVariable Long id) {
        courseServiceJpa.operationA(id);
        return ResponseEntity.ok().build();
    }


    /*
     * CHECKED EXCEPTION TEST
     *
     * Demonstrates rollbackFor = Exception.class.
     *
     * Normally a checked exception does not cause a rollback.
     * In this test, rollbackFor explicitly enables rollback.
     */
    @GetMapping("/test-checked/{id}")
    public ResponseEntity<Void> testChecked(@PathVariable Long id) throws Exception {
        courseServiceJpa.testCheckedException(id);
        return ResponseEntity.ok().build();
    }

}
