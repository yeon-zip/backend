package kr.ac.kumoh.polaris

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PolarisApplication

fun main(args: Array<String>) {
	runApplication<PolarisApplication>(*args)
}
