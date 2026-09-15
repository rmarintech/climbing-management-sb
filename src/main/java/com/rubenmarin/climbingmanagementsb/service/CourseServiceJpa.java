package com.rubenmarin.climbingmanagementsb.service;

import com.rubenmarin.climbingmanagementsb.event.CourseCreatedEvent;
import com.rubenmarin.climbingmanagementsb.event.CourseEventProducer;
import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.entity.CourseEntity;
import com.rubenmarin.climbingmanagementsb.exception.CourseNotFoundException;
import com.rubenmarin.climbingmanagementsb.exception.ExceptionMsg;
import com.rubenmarin.climbingmanagementsb.record.CourseRecord;
import com.rubenmarin.climbingmanagementsb.repository.CourseRepositoryJpa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseServiceJpa {

    private final CourseRepositoryJpa courseRepositoryJpa;
    private final TransactionTestService transactionTestService;
    private final CourseEventProducer courseEventProducer;

    public CourseServiceJpa(CourseRepositoryJpa courseRepositoryJpa, TransactionTestService transactionTestService, CourseEventProducer courseEventProducer) {
        this.courseRepositoryJpa = courseRepositoryJpa;
        this.transactionTestService = transactionTestService;
        this.courseEventProducer = courseEventProducer;
    }

    /*
     * Entity → Record
     *
     * The entity is the JPA/database representation.
     * The record is the object exposed by the API.
     */
    private CourseRecord toRecord(CourseEntity entity) {
        return new CourseRecord(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getDifficulty()
        );
    }


    /*
     * Record → Entity
     *
     * Used when receiving data from the API and creating a new persistent entity.
     */
    private CourseEntity toEntity(CourseRecord record) {
        return new CourseEntity(
                record.name(),
                record.price(),
                record.difficulty()
        );
    }

    public List<CourseRecord> findAll() {
        return courseRepositoryJpa.findAll()
                .stream()
                .map(this::toRecord)
                .toList();
    }

    public CourseRecord findById(Long id) {
        return courseRepositoryJpa.findById(id)
                .map(this::toRecord)
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                ExceptionMsg.COURSE_NOT_FOUND
                        )
                );
    }

    public CourseRecord create(CourseRecord courseRecord) {
        CourseEntity entity = toEntity(courseRecord);
        CourseEntity saved = courseRepositoryJpa.save(entity);

        // Create and publish the Kafka event after the entity has been saved,
        // because we need the generated database ID.
        CourseCreatedEvent event = CourseCreatedEvent.of(
                saved.getId(),
                saved.getName(),
                saved.getPrice(),
                saved.getDifficulty()
        );
        courseEventProducer.publishCourseCreated(event);

        return toRecord(saved);
    }

    /*
     * @Transactional starts a database transaction.
     *
     * The entity returned by findById() is managed by Hibernate inside the Persistence Context.
     * When we modify the entity, Hibernate detects the changes automatically through Dirty Checking.
     * At transaction commit, Hibernate generates the UPDATE SQL.
     * Therefore, calling repository.save(existing) is not necessary for an already managed entity.
     */
    @Transactional
    public CourseRecord update(Long id, CourseRecord courseRecord) {

        CourseEntity existing = courseRepositoryJpa.findById(id)
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                ExceptionMsg.COURSE_NOT_FOUND
                        )
                );

        existing.setName(courseRecord.name());
        existing.setPrice(courseRecord.price());
        existing.setDifficulty(courseRecord.difficulty());

        return toRecord(existing);
    }


    public CourseRecord delete(Long id) {
        CourseEntity existing = courseRepositoryJpa.findById(id)
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                ExceptionMsg.COURSE_NOT_FOUND
                        )
                );

        courseRepositoryJpa.delete(existing);
        return toRecord(existing);
    }


    /*
     * The repository method uses a Derived Query: findTopByOrderByPriceDesc()
     * Spring Data JPA derives the query from the method name.
     *
     * Conceptually:
     *
     * SELECT *
     * FROM courses
     * ORDER BY price DESC
     * LIMIT 1
     */
    public CourseRecord findMostExpensive() {
        CourseEntity mostExpensive = courseRepositoryJpa
                .findTopByOrderByPriceDesc()
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                ExceptionMsg.COURSE_NOT_FOUND
                        )
                );

        return toRecord(mostExpensive);
    }


    /*
     * Derived Query:
     * Spring Data JPA derives the SQL from the method name.
     */
    public List<CourseRecord> findByDifficulty(Difficulty difficulty) {

        return courseRepositoryJpa.findByDifficulty(difficulty)
                .stream()
                .map(this::toRecord)
                .toList();
    }


    /*
     * JPQL query defined with @Query in the repository.
     *
     * JPQL works with entities and their Java properties,
     * rather than directly with database tables and columns.
     */
    public List<CourseRecord> findByDifficultyAndPriceLessThan(Difficulty difficulty, Double price) {
        return courseRepositoryJpa
                .searchCourses(difficulty, price)
                .stream()
                .map(this::toRecord)
                .toList();
    }


    /*
     * TRANSACTIONAL ROLLBACK TEST
     *
     * @Transactional means both updates belong to the same transaction.
     * If the RuntimeException is thrown, the transaction is rolled back and neither change is persisted.
     * By default, Spring rolls back transactions for unchecked exceptions such as RuntimeException.
     */
    @Transactional
    public void testRequired(Long id1, Long id2) {

        CourseEntity course1 = courseRepositoryJpa.findById(id1)
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                ExceptionMsg.COURSE_1_NOT_FOUND
                        )
                );

        CourseEntity course2 = courseRepositoryJpa.findById(id2)
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                ExceptionMsg.COURSE_2_NOT_FOUND
                        )
                );

        course1.setPrice(111.0);
        course2.setPrice(222.0);

        throw new RuntimeException("Testing REQUIRED rollback");
    }

    /*
     * TRANSACTION PROPAGATION TEST
     *
     * operationA() starts a transaction.
     *
     * transactionTestService.operationB() is called from inside that transaction.
     * The behavior depends on the propagation configured in operationB().
     *
     * This is used to study REQUIRED vs REQUIRES_NEW.
     */
    @Transactional
    public void operationA(Long id) {
        CourseEntity course = courseRepositoryJpa.findById(id).orElseThrow(() -> new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));
        course.setName("CAMBIO DE A");
        transactionTestService.operationB(id);
        throw new RuntimeException("Rollback of A");
    }


    /*
     * CHECKED EXCEPTION TEST
     *
     * Spring's default rollback rules apply to unchecked exceptions (RuntimeException and Error),
     * but NOT normally to checked exceptions.
     *
     * This method is used to demonstrate that difference.
     */
    public void testCheckedException(Long id) throws Exception {
        transactionTestService.testCheckedException(id);
    }


    /*
     * PESSIMISTIC LOCKING
     *
     * PESSIMISTIC_WRITE requests a database-level lock on the selected row.
     *
     * Another transaction attempting a conflicting lock on the same row must wait until the current transaction completes.
     *
     * The lock is held for the duration of the transaction.
     *
     * The exact SQL depends on the database and Hibernate dialect.
     * With PostgreSQL, our test generated:
     *
     * SELECT ...
     * FROM courses
     * WHERE id = ?
     * FOR NO KEY UPDATE;
     */
    @Transactional
    public CourseEntity findByIdWithLock(Long id) {
        return courseRepositoryJpa.findByIdForUpdate(id).orElseThrow(() -> new CourseNotFoundException(ExceptionMsg.COURSE_NOT_FOUND));
    }
}