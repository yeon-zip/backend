package kr.ac.kumoh.polaris.global.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IsbnNormalizerTest {
    @Test
    fun `extract isbn13 from hyphenated isbn13`() {
        assertEquals("9788936434120", IsbnNormalizer.extractIsbn13("978-89-364-3412-0"))
    }

    @Test
    fun `extract isbn13 from isbn10 plus hyphenated isbn13`() {
        assertEquals("9788936434120", IsbnNormalizer.extractIsbn13("8936434120 978-89-364-3412-0"))
    }

    @Test
    fun `extract isbn13 from text prefixed hyphenated isbn13`() {
        assertEquals("9788936434120", IsbnNormalizer.extractIsbn13("ISBN 978-89-364-3412-0"))
    }

    @Test
    fun `extract isbn13 from isbn10 plus plain isbn13`() {
        assertEquals("9788936434120", IsbnNormalizer.extractIsbn13("8936434120 9788936434120"))
    }

    @Test
    fun `return null when no valid isbn13 candidate exists`() {
        assertNull(IsbnNormalizer.extractIsbn13("8936434120"))
        assertNull(IsbnNormalizer.extractIsbn13("ISBN-ONLY-TEXT"))
    }
}
