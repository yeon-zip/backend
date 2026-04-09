package kr.ac.kumoh.polaris.library.service

import kr.ac.kumoh.polaris.library.implement.LibraryUpsertProcessor
import kr.ac.kumoh.polaris.library.implement.client.Data4LibraryLibraryClient
import org.springframework.stereotype.Service

@Service
class LibrarySyncService(
    private val data4LibraryLibraryClient: Data4LibraryLibraryClient,
    private val libraryUpsertProcessor: LibraryUpsertProcessor
) {
    fun syncAll(): Int {
        var pageNo = 1
        var savedCount = 0

        while (true) {
            val pageResult = data4LibraryLibraryClient.searchLibraries(pageNo = pageNo, pageSize = 100)

            if (pageResult.libraries.isEmpty()) break

            pageResult.libraries.forEach { libraryDocument ->
                libraryUpsertProcessor.upsert(libraryDocument)
                savedCount++
            }

            pageNo++
        }

        return savedCount
    }
}
