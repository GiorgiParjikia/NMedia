package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    @Query("SELECT * FROM Post_Entity ORDER BY id DESC")
    fun getAll(): LiveData<List<PostEntity>>

    fun save(post: PostEntity) {
        if (post.id == 0L) {
            insert(post)
        } else {
            updateById(post.id, post.content)
        }
    }

    @Insert
    fun insert(post: PostEntity)

    @Query("UPDATE Post_Entity SET content = :content WHERE id = :id")
    fun updateById(id: Long, content: String)

    @Query(
        """
        UPDATE Post_Entity SET
            likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
            likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
        WHERE id = :id
        """
    )
    fun likeById(id: Long)

    @Query("DELETE FROM Post_Entity WHERE id = :id")
    fun removeById(id: Long)
}