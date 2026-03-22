package dev.haomin.resumer.app

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = ["dev.haomin.resumer.app"])
class AppApplication

fun main(args: Array<String>) {
	runApplication<AppApplication>(*args)
}
