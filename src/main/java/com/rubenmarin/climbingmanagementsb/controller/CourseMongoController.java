package com.rubenmarin.climbingmanagementsb.controller;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.dto.CourseDifficultyStatsDto;
import com.rubenmarin.climbingmanagementsb.dto.CourseMongoRequestDto;
import com.rubenmarin.climbingmanagementsb.dto.CourseMongoResponseDto;
import com.rubenmarin.climbingmanagementsb.dto.CourseWithEnrollmentsDto;
import com.rubenmarin.climbingmanagementsb.service.CourseMongoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mongo/courses")
public class CourseMongoController {

    private final CourseMongoService courseMongoService;

    public CourseMongoController(CourseMongoService courseMongoService) {
        this.courseMongoService = courseMongoService;
    }

    // GET http://localhost:8080/mongo/courses
    @GetMapping
    public Page<CourseMongoResponseDto> getCourses(Pageable pageable) {
        return courseMongoService.findAll(pageable);
    }

    // GET http://localhost:8080/mongo/courses/6a9ada74aed9b79d82d16295
    @GetMapping("/{id}")
    public CourseMongoResponseDto getCourseById(@PathVariable String id) {
        return courseMongoService.findById(id);
    }

    @PostMapping
    public ResponseEntity<CourseMongoResponseDto> createCourse(@Valid @RequestBody CourseMongoRequestDto course) {
        CourseMongoResponseDto created = courseMongoService.create(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public CourseMongoResponseDto updateCourse(@PathVariable String id, @Valid @RequestBody CourseMongoRequestDto course) {
        return courseMongoService.update(id, course);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        courseMongoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET http://localhost:8080/mongo/courses/difficulty/EASY
    @GetMapping("/difficulty/{difficulty}")
    public List<CourseMongoResponseDto> findByDifficulty(@PathVariable Difficulty difficulty) {
        return courseMongoService.findByDifficulty(difficulty);
    }

    // GET http://localhost:8080/mongo/courses/difficulty-lt-price/EASY/100
    @GetMapping("/difficulty-lt-price/{difficulty}/{price}")
    public List<CourseMongoResponseDto> findByDifficultyAndPriceLessThan(@PathVariable Difficulty difficulty, @PathVariable Double price) {
        return courseMongoService.findByDifficultyAndPriceLessThan(difficulty, price);
    }

    // GET http://localhost:8080/mongo/courses/difficulty-max-price-query?difficulty=EASY&price=100
    @GetMapping("/difficulty-max-price-query")
    public List<CourseMongoResponseDto> findCoursesByDifficultyAndMaxPriceQuery(@RequestParam Difficulty difficulty, @RequestParam Double price) {
        return courseMongoService.findCoursesByDifficultyAndMaxPriceQuery(difficulty, price);
    }

    // GET http://localhost:8080/mongo/courses/minimum-price?price=90
    @GetMapping("/minimum-price")
    public List<CourseMongoResponseDto> findCoursesWithMinimumPrice(@RequestParam Double price) {
        return courseMongoService.findCoursesWithMinimumPrice(price);
    }

    //GET http://localhost:8080/mongo/courses/difficultyIn?difficulties=EASY,MEDIUM
    @GetMapping("/difficultyIn")
    public List<CourseMongoResponseDto> findByDifficultyIn(@RequestParam List<Difficulty> difficulties) {
        return courseMongoService.findByDifficultyIn(difficulties);
    }

    // GET http://localhost:8080/mongo/courses/difficultyIn-query?difficulties=EASY,MEDIUM
    @GetMapping("/difficultyIn-query")
    public List<CourseMongoResponseDto> findByDifficultyInQuery(@RequestParam List<Difficulty> difficulties) {
        return courseMongoService.findByDifficultyInQuery(difficulties);
    }

    //http://localhost:8080/mongo/courses/difficultyNotIn-query?difficulties=HARD,MEDIUM
    @GetMapping("/difficultyNotIn-query")
    public List<CourseMongoResponseDto> findByDifficultyNotInQuery(@RequestParam List<Difficulty> difficulties) {
        return courseMongoService.findByDifficultyNotInQuery(difficulties);
    }

    // GET http://localhost:8080/mongo/courses/name-contains?name=ferr
    @GetMapping("/name-contains")
    public List<CourseMongoResponseDto> findByNameContainingIgnoreCase(@RequestParam String name) {
        return courseMongoService.findByNameContainingIgnoreCase(name);
    }

    //GET http://localhost:8080/mongo/courses/search?difficulty=MEDIUM&maxPrice=160&page=0&size=2&sort=price,asc
    @GetMapping("/search")
    public Page<CourseMongoResponseDto> searchCourses(@RequestParam Difficulty difficulty, @RequestParam Double maxPrice, Pageable pageable) {
        return courseMongoService.findByDifficultyAndPriceLessThanEqual(difficulty, maxPrice, pageable);
    }

    /*
     * DYNAMIC SEARCH WITH MONGOTEMPLATE
     *
     * All filters are optional.
     *
     * The service delegates the search to our custom repository, which uses MongoTemplate to build the MongoDB query dynamically.
     *
     * Available filters:
     * - name
     * - difficulty
     * - minPrice
     * - maxPrice
     *
     * Pageable also allows pagination and sorting.
     *
     * Examples:
     * GET /mongo/courses/dynamic-search
     * GET /mongo/courses/dynamic-search?difficulty=MEDIUM
     * GET /mongo/courses/dynamic-search?minPrice=100&maxPrice=160
     * GET /mongo/courses/dynamic-search?difficulty=MEDIUM&maxPrice=160&page=0&size=2&sort=price,asc
     */
    @GetMapping("/dynamic-search")
    public Page<CourseMongoResponseDto> searchDynamic(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Pageable pageable
    ) {
        return courseMongoService.search(name, difficulty, minPrice, maxPrice, pageable);
    }

    /*
     * MongoDB aggregation:
     * - filters courses with price >= 100
     * - groups them by difficulty
     * - calculates average price and course count
     * - sorts by average price descending
     * GET http://localhost:8080/mongo/courses/difficulty-stats
     */
    @GetMapping("/difficulty-stats")
    public List<CourseDifficultyStatsDto> getDifficultyStats() {
        return courseMongoService.getDifficultyStats();
    }

    /*
     * MongoDB $lookup:
     * joins courses with enrollments using:
     *
     * courses._id = enrollments.courseId
     *
     * GET http://localhost:8080/mongo/courses/with-enrollments
     */
    @GetMapping("/with-enrollments")
    public List<CourseWithEnrollmentsDto> findCoursesWithEnrollments() {
        return courseMongoService.findCoursesWithEnrollments();
    }

    /*
     * GET http://localhost:8080/mongo/courses/transaction-test
     */
    @PostMapping("/transaction-test")
    public ResponseEntity<Void> transactionTest() {
        courseMongoService.createCourseWithFailure();
        return ResponseEntity.ok().build();
    }

    /*
     * GET http://localhost:8080/mongo/courses/transaction-test2
     */
    @PostMapping("/transaction-test2")
    public ResponseEntity<Void> transactionTest2() {
        courseMongoService.createCourseWithEnrollmentAndFailure();
        return ResponseEntity.ok().build();
    }

}