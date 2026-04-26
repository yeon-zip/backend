package kr.ac.kumoh.polaris.bookvote.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import kr.ac.kumoh.polaris.bookvote.entity.BookVoteType

data class BookVoteRequest(
    @field:NotNull
    @field:Schema(
        description = "투표 값입니다. RECOMMEND 또는 NOT_RECOMMEND만 허용됩니다.",
        example = "RECOMMEND"
    )
    val voteType: BookVoteType
)
