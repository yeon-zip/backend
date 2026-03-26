package kr.ac.kumoh.polestar

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PolestarApplication

fun main(args: Array<String>) {
	runApplication<PolestarApplication>(*args)
}
