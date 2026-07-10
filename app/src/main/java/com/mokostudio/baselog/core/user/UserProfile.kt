package com.mokostudio.baselog.core.user

data class UserProfile(
    val nickname: String,
    val favoriteTeam: BaseballTeam,
    val bio: String,
    val email: String,
    val photoUrl: String
)

data class UserProfileDraft(
    val nickname: String,
    val favoriteTeam: BaseballTeam,
    val bio: String
)
