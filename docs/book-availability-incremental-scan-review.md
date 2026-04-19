# Book availability incremental-scan review

_Status_: lane 4 review/evidence note created from the current worker-3 branch on 2026-04-19 before any lane 1-3 code was integrated into this worktree. The note records the existing call path, why remote calls still scale with all libraries in radius, and what evidence should exist once the semantics-first incremental-scan patch lands.

## Current baseline call path

1. `BookAvailabilityService.getBookHoldings()` normalizes ISBN, builds the full holding list, then applies `openNow` filtering only after the list is complete (`src/main/kotlin/kr/ac/kumoh/polaris/bookavailability/service/BookAvailabilityService.kt:21-52`).
2. `buildHoldingItems()` delegates to `BookHoldingFinder.findByIsbn()` and only afterwards resolves open-now status for every returned holding (`.../BookAvailabilityService.kt:82-115`).
3. `BookHoldingFinder.findByIsbn()` currently uses `NearbyLibraryQueryRepository.findWithinRadius()` to load **all** nearby libraries in radius, then passes every distinct `libCode` to `LibraryBookAvailabilityChecker.checkAll()` (`src/main/kotlin/kr/ac/kumoh/polaris/bookavailability/implement/BookHoldingFinder.kt:15-34`).
4. `LibraryBookAvailabilityChecker.checkAll()` runs `fetchBookExistAsync()` for the full distinct lib-code list with fixed concurrency `8` (`src/main/kotlin/kr/ac/kumoh/polaris/bookavailability/implement/LibraryBookAvailabilityChecker.kt:38-89`).
5. `NearbyLibraryQueryRepository` already exposes `findPageWithinRadius(...)` with keyset pagination and `LIMIT :limit`, but book availability still calls the unbounded `findWithinRadius(...)` path instead (`src/main/kotlin/kr/ac/kumoh/polaris/library/implement/NearbyLibraryQueryRepository.kt:15-75`).

## Why the current implementation still over-calls Data4Library

From the baseline code above, one `/api/v1/book-availability` request can trigger up to:

- one full-radius SQL scan via `findWithinRadius(...)`, and
- one `bookExist` remote request per distinct library within that radius.

Because `openNow` filtering happens after all holdings are materialized, closed libraries still consume remote `bookExist` calls before they are filtered out. Likewise, pagination is applied after the full list is built (`BookAvailabilityService.kt:188-205`), so the current remote-call volume scales with **candidate libraries in radius**, not **`limit + 1` visible matches**.

## Expected properties of the approved patch

To satisfy the approved semantics-first design without changing API semantics, the merged implementation should show all of the following:

- book availability scans nearby libraries through `findPageWithinRadius(...)` rather than `findWithinRadius(...)`;
- the scan stops once it has collected `limit + 1` visible holdings after applying the existing `loanAvailable` and `openNow` semantics;
- `openNow` evaluation uses the existing `LibraryOpenStatusResolver` / `openNow` primitive during the scan instead of only after the full list is built;
- `(libCode, isbn)` responses are cached for real, so repeated scans do not re-hit Data4Library for the same pair;
- availability-check concurrency is configurable rather than fixed at `8`.

## Evidence checklist for the merged patch

Once lanes 1-3 land, the integrated branch should provide evidence for each item below.

### 1. Call-path evidence

Expected static indicators:

- no book-availability path still calls `findWithinRadius(...)` for the paged endpoint;
- the orchestration loop keeps a cursor/page boundary while accumulating visible matches;
- `openNow` and `loanAvailable` semantics are preserved before slicing to the public page response.

### 2. Remote-call bound evidence

A regression test should prove a case like the following:

- request limit = `2`;
- nearby page contains libraries that are skipped because they are closed / unavailable / missing the book;
- the implementation stops shortly after collecting the third visible match (`limit + 1`);
- the number of `bookExist` calls is bounded by the pages actually scanned, not by the full library count inside the radius.

A good integrated assertion is:

> when only three visible holdings are needed to decide the first page, `bookExist` is never called for libraries beyond the scan frontier required to obtain those three visible holdings.

### 3. Cache evidence

Expected verification:

- two identical `(libCode, isbn)` lookups in the same test scope produce a single underlying Data4Library invocation;
- cache keying is explicitly based on `libCode` + normalized `isbn`.

### 4. Configuration evidence

Expected verification:

- concurrency value can be overridden by application configuration;
- default value is documented and used when no override is supplied.

## Suggested verification commands after merge

```bash
./gradlew test
./gradlew compileKotlin
```

If lane 3 adds focused tests, prefer running that class directly as part of the evidence package as well.

## Review conclusion

As inspected in this worktree, the current baseline still performs full-radius availability resolution and therefore does **not** yet provide the approved bounded-call behavior. This note is intended to help the leader validate the final merged patch quickly by comparing the integrated code/tests against the exact over-call points identified above.
