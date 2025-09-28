package ru.netology.nmedia.dao

import ru.netology.nmedia.dto.Post

interface PostDao {
    fun getAll(): List<Post>
    fun save(post: Post): Post
    fun likeById(id: Long)
    fun shareById(id: Long)
    fun removeById(id: Long)

    companion object {
        const val DDL = """
            CREATE TABLE IF NOT EXISTS Posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                author TEXT NOT NULL,
                published TEXT NOT NULL,
                content TEXT NOT NULL,
                likes INTEGER NOT NULL DEFAULT 0,
                likeByMe INTEGER NOT NULL DEFAULT 0,
                shared INTEGER NOT NULL DEFAULT 0,
                views INTEGER NOT NULL DEFAULT 0,
                video TEXT
            );
        """
    }
}
