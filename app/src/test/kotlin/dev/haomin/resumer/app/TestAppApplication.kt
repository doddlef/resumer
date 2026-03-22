package dev.haomin.resumer.app

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<AppApplication>().with(TestcontainersConfiguration::class).run(*args)
}
