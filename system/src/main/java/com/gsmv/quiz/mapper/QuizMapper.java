package com.gsmv.quiz.mapper;

import com.gsmv.quiz.dto.QuizResultResponse;
import com.gsmv.quiz.model.QuizAnswer;
import com.gsmv.quiz.model.QuizQuestion;
import com.gsmv.quiz.model.QuizRecord;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.jdbc.SQL;

@Mapper
public interface QuizMapper {

    // ============ Question ============

    @SelectProvider(type = QuestionSqlBuilder.class, method = "findPage")
    List<QuizQuestion> findPage(
            @Param("category") String category,
            @Param("type") String type,
            @Param("difficulty") String difficulty,
            @Param("keyword") String keyword,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @SelectProvider(type = QuestionSqlBuilder.class, method = "count")
    long count(
            @Param("category") String category,
            @Param("type") String type,
            @Param("difficulty") String difficulty,
            @Param("keyword") String keyword
    );

    @SelectProvider(type = QuestionSqlBuilder.class, method = "findByFilters")
    List<QuizQuestion> findByFilters(
            @Param("categories") List<String> categories,
            @Param("difficulty") String difficulty,
            @Param("limit") int limit,
            @Param("random") boolean random
    );

    @Select("SELECT * FROM quiz_question WHERE id = #{id}")
    QuizQuestion findQuestionById(Long id);

    @Select("SELECT COUNT(*) FROM quiz_question WHERE title = #{title}")
    long countByTitle(@Param("title") String title);

    @Insert("INSERT INTO quiz_question (category, type, title, options, answer, explanation, difficulty) " +
            "VALUES (#{category}, #{type}, #{title}, #{options}, #{answer}, #{explanation}, #{difficulty})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertQuestion(QuizQuestion question);

    @Update("UPDATE quiz_question SET category=#{category}, type=#{type}, title=#{title}, options=#{options}, " +
            "answer=#{answer}, explanation=#{explanation}, difficulty=#{difficulty}, status=#{status} WHERE id=#{id}")
    void updateQuestion(QuizQuestion question);

    @Delete("DELETE FROM quiz_question WHERE id = #{id}")
    void deleteQuestion(Long id);

    // ============ Record ============

    @Insert("INSERT INTO quiz_record (user_id, score, total, categories, mode, finished_at) " +
            "VALUES (#{userId}, #{score}, #{total}, #{categories}, #{mode}, #{finishedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertRecord(QuizRecord record);

    @Update("UPDATE quiz_record SET score=#{score}, total=#{total}, finished_at=#{finishedAt} WHERE id=#{id}")
    void updateRecord(QuizRecord record);

    @Select("SELECT * FROM quiz_record WHERE id = #{id}")
    QuizRecord findRecordById(Long id);

    @Select("SELECT * FROM quiz_record WHERE user_id = #{userId} ORDER BY started_at DESC LIMIT #{size} OFFSET #{offset}")
    List<QuizRecord> findRecordsByUser(@Param("userId") Long userId, @Param("size") int size, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM quiz_record WHERE user_id = #{userId}")
    long countRecordsByUser(Long userId);

    // ============ Answer ============

    @Insert("INSERT INTO quiz_answer (record_id, question_id, user_answer, is_correct) " +
            "VALUES (#{recordId}, #{questionId}, #{userAnswer}, #{isCorrect})")
    void insertAnswer(QuizAnswer answer);

    @Select("SELECT * FROM quiz_answer WHERE record_id = #{recordId}")
    List<QuizAnswer> findAnswersByRecord(Long recordId);

    @Delete("DELETE FROM quiz_answer WHERE record_id = #{recordId}")
    void deleteAnswersByRecord(Long recordId);

    // ============ Stats ============

    @Select("SELECT COUNT(*) FROM quiz_question WHERE status = 1 AND category = #{category}")
    long countByCategory(String category);

    class QuestionSqlBuilder {

        public String findPage(@Param("category") String category, @Param("type") String type,
                               @Param("difficulty") String difficulty, @Param("keyword") String keyword,
                               @Param("size") int size, @Param("offset") int offset) {
            return buildSelect(category, type, difficulty, keyword, false) +
                   " ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}";
        }

        public String count(@Param("category") String category, @Param("type") String type,
                            @Param("difficulty") String difficulty, @Param("keyword") String keyword) {
            return buildSelect(category, type, difficulty, keyword, true);
        }

        public String findByFilters(@Param("categories") List<String> categories,
                                     @Param("difficulty") String difficulty,
                                     @Param("limit") int limit,
                                     @Param("random") boolean random) {
            StringBuilder sql = new StringBuilder("SELECT * FROM quiz_question WHERE status = 1");
            if (categories != null && !categories.isEmpty()) {
                sql.append(" AND category IN (");
                for (int i = 0; i < categories.size(); i++) {
                    sql.append("#{categories[").append(i).append("]}");
                    if (i < categories.size() - 1) sql.append(", ");
                }
                sql.append(")");
            }
            if (difficulty != null && !difficulty.isEmpty()) {
                sql.append(" AND difficulty = #{difficulty}");
            }
            if (random) {
                sql.append(" ORDER BY RAND()");
            }
            sql.append(" LIMIT #{limit}");
            return sql.toString();
        }

        private String buildSelect(String category, String type, String difficulty, String keyword, boolean isCount) {
            StringBuilder sql = new StringBuilder();
            sql.append(isCount ? "SELECT COUNT(*) FROM quiz_question WHERE status = 1" :
                                  "SELECT * FROM quiz_question WHERE status = 1");
            if (category != null && !category.isEmpty()) {
                sql.append(" AND category = #{category}");
            }
            if (type != null && !type.isEmpty()) {
                sql.append(" AND type = #{type}");
            }
            if (difficulty != null && !difficulty.isEmpty()) {
                sql.append(" AND difficulty = #{difficulty}");
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND title LIKE CONCAT('%', #{keyword}, '%')");
            }
            return sql.toString();
        }
    }
}
