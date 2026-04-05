package kr.ac.kumoh.polestar.global.exception

class ServiceException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)
