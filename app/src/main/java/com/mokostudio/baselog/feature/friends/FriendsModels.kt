package com.mokostudio.baselog.feature.friends

import com.mokostudio.baselog.core.user.BaseballTeam

data class FriendSummary(
    val userId: String,
    val nickname: String,
    val favoriteTeam: BaseballTeam,
    val bio: String,
    val photoUrl: String
)

data class FriendRequest(
    val requestId: String,
    val requester: FriendSummary
)

enum class FriendRequestStatus {
    Pending,
    Accepted,
    Rejected
}
