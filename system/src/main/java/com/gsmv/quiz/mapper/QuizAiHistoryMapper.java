package com.gsmv.quiz.mapper;

import com.gsmv.quiz.model.QuizAiChatMessage;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuizAiHistoryMapper {

    @Insert("""
            INSERT INTO quiz_ai_chat_message (
              user_id, role, content
            ) VALUES (
              #{userId}, #{role}, #{content}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(QuizAiChatMessage message);

    @Select("""
            SELECT *
            FROM (
              SELECT *
              FROM quiz_ai_chat_message
              WHERE user_id = #{userId}
              ORDER BY created_at DESC, id DESC
              LIMIT #{limit}
            ) recent_messages
            ORDER BY created_at ASC, id ASC
            """)
    List<QuizAiChatMessage> listRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Delete("DELETE FROM quiz_ai_chat_message WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
