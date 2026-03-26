package kr.ac.kumoh.polestar.global.response

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.*
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.module.kotlin.KotlinInvalidNullException

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val httpStatus = HttpStatus.BAD_REQUEST

        val problemDetail = ProblemDetail.forStatus(httpStatus).apply {
            title = httpStatus.reasonPhrase
            detail = when (val cause = ex.cause) {
                is KotlinInvalidNullException -> "${cause.kotlinPropertyName}: null일 수 없습니다."
                is InvalidFormatException -> "${cause.path.lastOrNull()?.propertyName.orEmpty()}: 올바른 형식이어야 합니다."
                else -> cause?.message.orEmpty()
            }
        }

        return ResponseEntity.status(httpStatus)
            .body(problemDetail)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val httpStatus = HttpStatus.BAD_REQUEST

        val problemDetail = ProblemDetail.forStatus(httpStatus).apply {
            title = httpStatus.reasonPhrase
            detail = ex.message
        }

        return ResponseEntity.status(httpStatus)
            .body(problemDetail)
    }

    override fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any> {
        val httpStatus = HttpStatus.NOT_FOUND

        val problemDetail = ProblemDetail.forStatus(httpStatus).apply {
            title = httpStatus.reasonPhrase
            detail = "경로 ${ex.httpMethod}:/${ex.resourcePath}을(를) 찾을 수 없습니다."
        }

        return ResponseEntity.status(httpStatus)
            .body(problemDetail)
    }

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBadRequestException(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> {
        logger.error("message", ex)

        val httpStatus = HttpStatus.BAD_REQUEST

        val problemDetail = ProblemDetail.forStatus(httpStatus).apply {
            title = httpStatus.reasonPhrase
            detail = ex.message
        }

        return ResponseEntity.status(httpStatus)
            .body(problemDetail)
    }

    @ExceptionHandler(NoSuchElementException::class, EntityNotFoundException::class)
    fun handleNotFoundException(ex: NoSuchElementException): ResponseEntity<ProblemDetail> {
        logger.error("message", ex)

        val httpStatus = HttpStatus.NOT_FOUND

        val problemDetail = ProblemDetail.forStatus(httpStatus).apply {
            title = httpStatus.reasonPhrase
            detail = ex.message
        }

        return ResponseEntity.status(httpStatus)
            .body(problemDetail)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception): ResponseEntity<ProblemDetail> {
        logger.error("message", ex)

        val httpStatus = HttpStatus.INTERNAL_SERVER_ERROR

        val problemDetail = ProblemDetail.forStatus(httpStatus).apply {
            title = httpStatus.reasonPhrase
        }

        return ResponseEntity.status(httpStatus)
            .body(problemDetail)
    }
}
