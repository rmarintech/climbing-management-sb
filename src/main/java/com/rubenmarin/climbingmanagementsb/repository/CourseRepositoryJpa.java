package com.rubenmarin.climbingmanagementsb.repository;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.entity.CourseEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

// JpaRepository provides these CRUD methods out of the box:
//
// save(entity)              -> Create or update an entity
// saveAll(entities)         -> Create or update multiple entities
//
// findById(id)              -> Find one entity by ID
// findAll()                 -> Find all entities
// findAllById(ids)          -> Find entities by multiple IDs
//
// deleteById(id)            -> Delete an entity by ID
// delete(entity)            -> Delete a specific entity
// deleteAll()               -> Delete all entities
// deleteAllById(ids)        -> Delete multiple entities by ID
//
// existsById(id)            -> Check if an entity exists
// count()                   -> Count entities
//
// flush()                   -> Flush changes to the database
// saveAndFlush(entity)      -> Save entity and immediately flush
// deleteAllInBatch()        -> Delete all entities in a batch
// deleteAllByIdInBatch(ids) -> Delete entities by IDs in a batch
// deleteInBatch(entities)   -> Delete entities in a batch
//
// Spring Data JPA provides the implementation automatically. No implementation is required.
//
// We can extend JpaRepository with:
//
// 1. Derived queries:
//    findByName(...)
//    findByDifficulty(...)
//    findByDifficultyAndPriceLessThan(...)
//
// 2. JPQL / custom queries:
//    @Query("SELECT c FROM Course c WHERE ...")
//
// 3. Native SQL queries:
//    @Query(value = "SELECT * FROM courses WHERE ...",nativeQuery = true)

public interface CourseRepositoryJpa extends JpaRepository<CourseEntity, Long> {


    /*
     * DERIVED QUERY
     *
     * Spring Data JPA derives the query from the method name.
     *
     * findByDifficulty(...)
     *       ↓
     * looks for the "difficulty" property in CourseEntity
     *       ↓
     * generates the corresponding SQL automatically.
     *
     * Conceptually:
     *
     * SELECT *
     * FROM courses
     * WHERE difficulty = ?
     */
    List<CourseEntity> findByDifficulty(Difficulty difficulty);


    /*
     * DERIVED QUERY
     *
     * findTopByOrderByPriceDesc()
     *
     * Spring interprets:
     *   Top       → return the first result
     *   OrderBy   → sort by a property
     *   Price     → property to sort
     *   Desc      → descending order
     *
     * Conceptually:
     *
     * SELECT *
     * FROM courses
     * ORDER BY price DESC
     * LIMIT 1
     */
    Optional<CourseEntity> findTopByOrderByPriceDesc();


    /*
     * DERIVED QUERY
     *
     * Spring Data can combine multiple conditions directly from the method name.
     *
     * Difficulty + And + Price + LessThan
     *
     * Conceptually:
     *
     * SELECT *
     * FROM courses
     * WHERE difficulty = ?
     * AND price < ?
     *
     * This method is kept as an example of a more complex
     * Derived Query.
     */
    List<CourseEntity> findByDifficultyAndPriceLessThan(         Difficulty difficulty,            Double price    );


    /*
     * JPQL
     *
     * @Query allows us to explicitly define the query.
     *
     * JPQL works with ENTITY classes and their Java properties,NOT directly with database table/column names.
     *
     * CourseEntity → entity
     * c.difficulty → Java entity property
     * c.price      → Java entity property
     *
     * This is different from native SQL.
     */
    @Query("""
            SELECT c
            FROM CourseEntity c
            WHERE c.difficulty = :difficulty
            AND c.price < :price
            """)
    List<CourseEntity> searchCourses(
            @Param("difficulty") Difficulty difficulty,
            @Param("price") Double price
    );


    /*
     * PESSIMISTIC LOCKING
     *
     * PESSIMISTIC_WRITE requests a database-level lock on the selected entity.

     * Another transaction attempting a conflicting operation on the same row must wait until the current transaction completes.
     * The lock is held for the duration of the transaction.

     * Hibernate/PostgreSQL generated SQL during our test:
     *
     * SELECT ...
     * FROM courses
     * WHERE id = ?
     * FOR NO KEY UPDATE;
     *
     * The exact SQL depends on the database and Hibernate dialect.
     *
     * Important:
     * The lock is held by the DATABASE TRANSACTION, not by Java or by the repository method itself.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CourseEntity c WHERE c.id = :id")
    Optional<CourseEntity> findByIdForUpdate(@Param("id") Long id);
}