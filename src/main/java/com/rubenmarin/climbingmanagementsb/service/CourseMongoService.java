package com.rubenmarin.climbingmanagementsb.service;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.document.CourseMongoDocument;
import com.rubenmarin.climbingmanagementsb.document.EnrollmentMongoDocument;
import com.rubenmarin.climbingmanagementsb.dto.CourseDifficultyStatsDto;
import com.rubenmarin.climbingmanagementsb.dto.CourseMongoRequestDto;
import com.rubenmarin.climbingmanagementsb.dto.CourseMongoResponseDto;
import com.rubenmarin.climbingmanagementsb.dto.CourseWithEnrollmentsDto;
import com.rubenmarin.climbingmanagementsb.exception.CourseNotFoundException;
import com.rubenmarin.climbingmanagementsb.exception.ExceptionMsg;
import com.rubenmarin.climbingmanagementsb.repository.CourseMongoRepository;

import com.rubenmarin.climbingmanagementsb.repository.EnrollmentMongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseMongoService {

    private final CourseMongoRepository courseMongoRepository;
    private final EnrollmentMongoRepository enrollmentMongoRepository;

    public CourseMongoService(CourseMongoRepository courseMongoRepository, EnrollmentMongoRepository enrollmentMongoRepository) {
        this.courseMongoRepository = courseMongoRepository;
        this.enrollmentMongoRepository = enrollmentMongoRepository;

    }

    private CourseMongoResponseDto toDtoResponse(CourseMongoDocument course) {
        return new CourseMongoResponseDto(course.getId(), course.getName(), course.getPrice(), course.getDifficulty());
    }

    private CourseMongoDocument toDocument(CourseMongoRequestDto courseRequest) {
        return new CourseMongoDocument(courseRequest.getName(), courseRequest.getPrice(), courseRequest.getDifficulty());
    }


    private CourseMongoDocument findDocumentById(String id) {
        return courseMongoRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));
    }

    // Spring automatically reads: ?page=0&size=10&sort=price,asc
    // GET http://localhost:8080/mongo/courses?page=0&size=2
    // Cheapest → most expensive: GET http://localhost:8080/mongo/courses?page=0&size=10&sort=price,asc
    // Most expensive → cheapest: GET http://localhost:8080/mongo/courses?page=0&size=10&sort=price,desc
    // Alphabetical by name: GET http://localhost:8080/mongo/courses?page=0&size=10&sort=name,asc
    public Page<CourseMongoResponseDto> findAll(Pageable pageable) {
        return courseMongoRepository.findAll(pageable).map(this::toDtoResponse);
    }

    public CourseMongoResponseDto findById(String id) {
        CourseMongoDocument course = findDocumentById(id);
        return toDtoResponse(course);
    }

    public CourseMongoResponseDto create(CourseMongoRequestDto courseRq) {
        CourseMongoDocument course = toDocument(courseRq);
        CourseMongoDocument saved = courseMongoRepository.save(course);
        return toDtoResponse(saved);
    }

    public CourseMongoResponseDto update(String id, CourseMongoRequestDto courseRq) {
        CourseMongoDocument existing = findDocumentById(id);

        existing.setName(courseRq.getName());
        existing.setPrice(courseRq.getPrice());
        existing.setDifficulty(courseRq.getDifficulty());

        CourseMongoDocument saved = courseMongoRepository.save(existing);
        return toDtoResponse(saved);
    }

    public void delete(String id) {
        courseMongoRepository.deleteById(id);
    }

    public List<CourseMongoResponseDto> findByDifficulty(Difficulty difficulty) {
        return courseMongoRepository.findByDifficulty(difficulty).stream().map(this::toDtoResponse).toList();
    }

    public List<CourseMongoResponseDto> findByDifficultyAndPriceLessThan(Difficulty difficulty, Double price) {
        return courseMongoRepository.findByDifficultyAndPriceLessThan(difficulty, price).stream().map(this::toDtoResponse).toList();
    }

    public List<CourseMongoResponseDto> findCoursesByDifficultyAndMaxPriceQuery(Difficulty difficulty, Double price) {
        return courseMongoRepository.findCoursesByDifficultyAndMaxPriceQuery(difficulty, price).stream().map(this::toDtoResponse).toList();
    }

    public List<CourseMongoResponseDto> findCoursesWithMinimumPrice(Double price) {
        return courseMongoRepository.findCoursesWithMinimumPriceQuery(price).stream().map(this::toDtoResponse).toList();
    }

    public List<CourseMongoResponseDto> findByDifficultyIn(List<Difficulty> difficulties) {
        return courseMongoRepository.findByDifficultyIn(difficulties).stream().map(this::toDtoResponse).toList();
    }

    public List<CourseMongoResponseDto> findByDifficultyInQuery(List<Difficulty> difficulties) {
        return courseMongoRepository.findByDifficultyInQuery(difficulties).stream().map(this::toDtoResponse).toList();
    }

    public List<CourseMongoResponseDto> findByDifficultyNotInQuery(List<Difficulty> difficulties) {
        return courseMongoRepository.findByDifficultyNotInQuery(difficulties).stream().map(this::toDtoResponse).toList();
    }

    public List<CourseMongoResponseDto> findByNameContainingIgnoreCase(String name) {
        return courseMongoRepository.findByNameContainingIgnoreCase(name).stream().map(this::toDtoResponse).toList();
    }

    public Page<CourseMongoResponseDto> findByDifficultyAndPriceLessThanEqual(Difficulty difficulty, Double price, Pageable pageable) {
        return courseMongoRepository.findByDifficultyAndPriceLessThanEqual(difficulty, price, pageable).map(this::toDtoResponse);
    }

    // Use MongoTemplate for dynamic queries
    public Page<CourseMongoResponseDto> search(String name, Difficulty difficulty, Double minPrice, Double maxPrice, Pageable pageable) {
        return courseMongoRepository.search(name, difficulty, minPrice, maxPrice, pageable).map(this::toDtoResponse);
    }

    // Aggregation: Match - Group - Project
    public List<CourseDifficultyStatsDto> getDifficultyStats() {
        return courseMongoRepository.getDifficultyStats();
    }

    // Aggregation: lookup - project
    public List<CourseWithEnrollmentsDto> findCoursesWithEnrollments() {
        return courseMongoRepository.findCoursesWithEnrollments();
    }


    //transactions on MongoDB requires to create different nodes
    @Transactional
    public void createCourseWithFailure() {
        CourseMongoDocument course = new CourseMongoDocument("Transactional Course", 200.0, Difficulty.HARD);
        courseMongoRepository.save(course);

        // Force an exception after the first write.
        throw new RuntimeException("Simulated transaction failure");
    }

    //transactions on MongoDB requires to create different nodes
    @Transactional
    public void createCourseWithEnrollmentAndFailure() {
        CourseMongoDocument course = new CourseMongoDocument("Transactional Course", 200.0, Difficulty.HARD);
        courseMongoRepository.save(course);

        EnrollmentMongoDocument enrollment = new EnrollmentMongoDocument(course.getId(), "Transactional Student");
        enrollmentMongoRepository.save(enrollment);

        // Simulate a failure after both writes.
        throw new RuntimeException("Simulated transaction failure");
    }

}
