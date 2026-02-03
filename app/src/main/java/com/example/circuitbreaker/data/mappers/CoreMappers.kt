package com.example.circuitbreaker.data.mappers

import com.example.circuitbreaker.data.dto.PostDto
import com.example.circuitbreaker.data.dto.UserDto
import com.example.circuitbreaker.domain.model.Post
import com.example.circuitbreaker.domain.model.User


fun UserDto.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        phone = phone ?: "" ,
        website = website ?: "",
        username = username ?: ""
    )
}


fun PostDto.toDomain(): Post {
    return Post(
        id = id,
        userId = userId,
        title = title,
        body = body
    )
}

/*


fun List<UserDto>.toDomain(): List<User> =
    map { it.toDomain() }



fun List<PostDto>.toDomain(): List<Post> =
    map { it.toDomain() }*/
