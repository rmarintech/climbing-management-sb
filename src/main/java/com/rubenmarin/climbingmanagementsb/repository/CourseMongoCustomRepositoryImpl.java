package com.rubenmarin.climbingmanagementsb.repository;

import com.rubenmarin.climbingmanagementsb.model.Difficulty;
import com.rubenmarin.climbingmanagementsb.document.CourseMongoDocument;
import com.rubenmarin.climbingmanagementsb.dto.CourseDifficultyStatsDto;
import com.rubenmarin.climbingmanagementsb.dto.CourseWithEnrollmentsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

public class CourseMongoCustomRepositoryImpl implements CourseMongoCustomRepository {

    private final MongoTemplate mongoTemplate;

    public CourseMongoCustomRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<CourseMongoDocument> search(String name, Difficulty difficulty, Double minPrice, Double maxPrice, Pageable pageable) {
        Query query = new Query();

        if (name != null && !name.isBlank()) {
            query.addCriteria(Criteria.where("name").regex(name, "i"));
        }

        if (difficulty != null) {
            query.addCriteria(Criteria.where("difficulty").is(difficulty));
        }

        if (minPrice != null) {
            query.addCriteria(Criteria.where("price").gte(minPrice));
        }

        if (maxPrice != null) {
            query.addCriteria(Criteria.where("price").lte(maxPrice));
        }

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), CourseMongoDocument.class);

        /*
         * Apply pagination and sorting from Pageable to the MongoDB query.
         *
         * Pageable contains:
         * - page number / offset
         * - page size
         * - sorting information
         *
         * same as query.with(pageable);
         */

        query.skip(pageable.getOffset());
        query.limit(pageable.getPageSize());
        query.with(pageable.getSort());

        List<CourseMongoDocument> courses = mongoTemplate.find(query, CourseMongoDocument.class);

        return PageableExecutionUtils.getPage(courses, pageable, () -> total);
    }


    /*
     * Build an aggregation pipeline equivalent to:
     *
     * db.courses.aggregate([
     *     { $match: { price: { $gte: 100 } } },
     *     { $group: {
     *           _id: "$difficulty",
     *           averagePrice: { $avg: "$price" },
     *           courseCount: { $sum: 1 }
     *     }},
     *     { $project: {
     *           _id: 0,
     *           difficulty: "$_id",
     *           averagePrice: 1,
     *           courseCount: 1
     *     }},
     *     { $sort: { averagePrice: -1 } }
     * ])
     */
    @Override
    public List<CourseDifficultyStatsDto> getDifficultyStats() {
        Aggregation aggregation = Aggregation.newAggregation(

                // 1. Filter documents before grouping.
                Aggregation.match(Criteria.where("price").gte(100)),

                // 2. Group courses by difficulty and calculate statistics.
                Aggregation.group("difficulty")
                        .avg("price").as("averagePrice")
                        .count().as("courseCount"),

                // 3. Reshape the aggregation result.
                //    MongoDB stores the group key in "_id", so map it to "difficulty".
                Aggregation.project()
                        .and("_id").as("difficulty")
                        .and("averagePrice").as("averagePrice")
                        .and("courseCount").as("courseCount")
                        .andExclude("_id"),

                // 4. Sort results by average price, highest first.
                Aggregation.sort(Sort.Direction.DESC, "averagePrice"));

        // Execute the aggregation and map each result to our DTO.
        // Aggregation = pipeline builder, MongoTemplate.aggregate() = executes the pipeline, DTO = receives the mapped results.
        return mongoTemplate.aggregate(aggregation, CourseMongoDocument.class, CourseDifficultyStatsDto.class).getMappedResults();
    }

    /*
     * Retrieves courses together with their enrollments.
     *
     * The custom repository uses MongoTemplate and a $lookup aggregation to join courses with the enrollments collection.
     */
    @Override
    public List<CourseWithEnrollmentsDto> findCoursesWithEnrollments() {

        Aggregation aggregation = Aggregation.newAggregation(

                /*
                 * Join the courses collection with the enrollments collection.
                 *
                 * courses._id
                 *      ↓
                 * enrollments.courseId
                 *
                 * The matching enrollments are stored in the "enrollments" array.
                 */
                Aggregation.lookup(
                        "enrollments",
                        "_id",
                        "courseId",
                        "enrollments"
                ),

                /*
                 * Select the fields that we want to expose.
                 *
                 * The aggregation result is mapped directly to CourseWithEnrollmentsDto.
                 */
                Aggregation.project()
                        .and("_id").as("id")
                        .and("name").as("name")
                        .and("price").as("price")
                        .and("difficulty").as("difficulty")
                        .and("enrollments").as("enrollments")
        );

        return mongoTemplate.aggregate(aggregation, CourseMongoDocument.class, CourseWithEnrollmentsDto.class).getMappedResults();
    }
}
