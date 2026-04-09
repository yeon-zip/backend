package kr.ac.kumoh.polaris.global.exception

class ServiceException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)
