package com.rubenmarin.climbingmanagementsb.repository;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.document.CourseMongoDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

// MongoRepository provides these CRUD methods out of the box:
//
// save(entity)              -> Create or update a document
// findById(id)              -> Find one document by ID
// findAll()                 -> Find all documents
// findAllById(ids)          -> Find documents by multiple IDs
// deleteById(id)            -> Delete a document by ID
// delete(entity)            -> Delete a specific document
// deleteAll()               -> Delete all documents
// deleteAllById(ids)        -> Delete multiple documents by ID
// existsById(id)            -> Check if a document exists
// count()                   -> Count documents
//
// Spring Data MongoDB provides the implementation automatically. No implementation is required.
//
// We can extend MongoRepository with:
//
// 1. Derived queries:
//    findByDifficulty(...)
//    findByDifficultyAndPriceLessThan(...)
//
// 2. Custom queries:
//    @Query("{ ... }")

public interface CourseMongoRepository extends MongoRepository<CourseMongoDocument, String>, CourseMongoCustomRepository {

    List<CourseMongoDocument> findByDifficulty(Difficulty difficulty);

    List<CourseMongoDocument> findByDifficultyAndPriceLessThan(Difficulty difficulty, Double price);


    // ?0 → first method parameter
    // ?1 → second method parameter
    // MongoDB operators: $gt, $gte, $lt, $lte, $in, $ne, $regex
    // db.courses.find({difficulty: "HARD",price: { $lt: 100 }})

    @Query("{ 'difficulty': ?0, 'price': { $lt: ?1 } }")
    List<CourseMongoDocument> findCoursesByDifficultyAndMaxPriceQuery(Difficulty difficulty, Double price);

    @Query("{ 'price': { $gte: ?0 } }")
    List<CourseMongoDocument> findCoursesWithMinimumPriceQuery(Double price);

    List<CourseMongoDocument> findByDifficultyIn(List<Difficulty> difficulties);

    // courses whose difficulty is either EASY or MEDIUM.  (WHERE difficulty IN ('EASY', 'MEDIUM'))
    @Query("{ 'difficulty': { $in: ?0 } }")
    List<CourseMongoDocument> findByDifficultyInQuery(List<Difficulty> difficulties);

    // courses whose difficulty is not HARD.
    @Query("{ 'difficulty': { $nin: ?0 } }")
    List<CourseMongoDocument> findByDifficultyNotInQuery(List<Difficulty> difficulties);

    //WHERE name LIKE '%ferrata%'
    List<CourseMongoDocument> findByNameContainingIgnoreCase(String name);

    // Paginated results
    Page<CourseMongoDocument> findByDifficultyAndPriceLessThanEqual(Difficulty difficulty, Double price, Pageable pageable);



}