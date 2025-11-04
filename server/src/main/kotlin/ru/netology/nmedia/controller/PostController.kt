package ru.netology.nmedia.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.service.PostService

@RestController
@RequestMapping("/api/posts", "/api/slow/posts")
class PostController(private val service: PostService) {

    @GetMapping
    fun getAll(): List<Post> {
        if (Math.random() < 0.5) {
            throw RuntimeException("Simulated server error (500)")
        }
        return service.getAll()
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) = service.getById(id)

    @PostMapping
    fun save(@RequestBody dto: Post): Post {
        if (Math.random() < 0.2) {
            throw RuntimeException("Simulated failure during save()")
        }
        return service.save(dto)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeById(@PathVariable id: Long) {
        if (Math.random() < 0.2) {
            throw RuntimeException("Simulated delete failure")
        }
        service.removeById(id)
    }

    @PostMapping("/{id}/likes")
    fun likeById(@PathVariable id: Long): Post {
        if (Math.random() < 0.3) {
            throw RuntimeException("Simulated like error")
        }
        return service.likeById(id)
    }

    @DeleteMapping("/{id}/likes")
    fun unlikeById(@PathVariable id: Long): Post {
        if (Math.random() < 0.3) {
            throw RuntimeException("Simulated unlike error")
        }
        return service.unlikeById(id)
    }
}
