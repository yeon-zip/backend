package kr.ac.kumoh.polaris.bookavailability.implement

import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistClient
import kr.ac.kumoh.polaris.bookavailability.implement.client.Data4LibraryBookExistResult
import kr.ac.kumoh.polaris.bookavailability.implement.dto.LibraryBookAvailabilityResult
import kr.ac.kumoh.polaris.global.properties.Data4LibraryApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class LibraryBookAvailabilityChecker(
    private val libraryBookAvailabilityReader: LibraryBookAvailabilityReader,
    private val properties: Data4LibraryApiProperties
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun check(
        libCode: String,
        isbn: String
    ): LibraryBookAvailabilityResult {
        log.info("도서 소장 여부 확인 시작 - libCode={}, isbn={}", libCode, isbn)

        val result = libraryBookAvailabilityReader.read(
            libCode = libCode,
            isbn = isbn
        )

        log.info(
            "도서 소장 여부 확인 완료 - libCode={}, isbn={}, hasBook={}, loanAvailable={}",
            libCode,
            isbn,
            result.hasBook,
            result.loanAvailable
        )

        return toAvailabilityResult(result)
    }

    fun checkAll(
        libCodes: Collection<String>,
        isbn: String
    ): Map<String, LibraryBookAvailabilityResult> {
        val distinctLibCodes = libCodes.distinct()
        if (distinctLibCodes.isEmpty()) {
            return emptyMap()
        }

        val startedAt = System.nanoTime()
        val concurrency = properties.bookExistConcurrency.coerceAtLeast(1)
        val results = Flux.fromIterable(distinctLibCodes)
            .flatMapSequential(
                { libCode ->
                    Mono.fromCallable {
                        libraryBookAvailabilityReader.read(
                            libCode = libCode,
                            isbn = isbn
                        )
                    }.subscribeOn(Schedulers.boundedElastic())
                },
                concurrency
            )
            .collectMap(
                { result -> result.libCode },
                { result -> toAvailabilityResult(result) }
            )
            .blockOptional()
            .orElseGet(::emptyMap)

        log.info(
            "도서 소장 여부 병렬 확인 완료 - isbn={}, candidateCount={}, resolvedCount={}, concurrency={}, elapsedMs={}",
            isbn,
            distinctLibCodes.size,
            results.size,
            concurrency,
            elapsedMillis(startedAt)
        )

        return results
    }

    private fun toAvailabilityResult(result: Data4LibraryBookExistResult) =
        LibraryBookAvailabilityResult(
            hasBook = result.hasBook,
            loanAvailable = result.loanAvailable,
            status = result.status
        )

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt) / 1_000_000
}
