package kr.ac.kumoh.polaris

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@Disabled("Requires local MariaDB/test environment; targeted unit tests cover current regression work")
@SpringBootTest(classes = [PolarisApplication::class])
class PolestarApplicationTests {

	@Test
	fun contextLoads() {
	}

}
