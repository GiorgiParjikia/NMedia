package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    // Получение всех постов (LiveData)
    @Query("SELECT * FROM Post_Entity ORDER BY id DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Query("SELECT COUNT(*) = 0 FROM Post_Entity")
    fun isEmpty(): LiveData<Boolean>

    // Сохранение поста (вставка или обновление)
    suspend fun save(post: PostEntity) {
        if (post.id == 0L) {
            insert(post)
        } else {
            updateById(post.id, post.content)
        }
    }

    // Вставка одного поста
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    // Добавляем перегруженный метод для вставки списка постов
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    // Обновление по ID
    @Query("UPDATE Post_Entity SET content = :content WHERE id = :id")
    suspend fun updateById(id: Long, content: String)

    // Лайк/дизлайк по ID
    @Query(
        """
        UPDATE Post_Entity SET
            likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    suspend fun likeById(id: Long)

    // Удаление по ID
    @Query("DELETE FROM Post_Entity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("SELECT * FROM Post_Entity WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Long): PostEntity?

}