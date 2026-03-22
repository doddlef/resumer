import org.jooq.meta.jaxb.*

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		// Flyway Gradle tasks resolve DB support from the buildscript classpath.
		classpath("org.flywaydb:flyway-database-postgresql:11.15.0")
		classpath("org.postgresql:postgresql:42.7.10")
	}
}

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.flyway)
	alias(libs.plugins.asciidoctor.jvm)
	alias(libs.plugins.jooq.codegen)
}

group = "dev.haomin.resumer"
version = "0.0.1-SNAPSHOT"

extra["jooq.version"] = libs.versions.jooq.get().toString()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get().toInt())
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
	// Main Spring Boot
	implementation(libs.spring.boot.starter.data.redis)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.jooq)
	implementation(libs.spring.boot.starter.mail)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.webmvc)

	// Main
	implementation(libs.flyway.database.postgresql)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.jjwt.api)
	implementation(libs.jooq.postgres.extension)
	implementation(libs.kotlin.reflect)
	implementation(libs.lettuce.core)
	implementation(libs.tika.core)
	implementation(libs.tika.parsers)
	implementation(libs.uuid.creator)

	// Development
	developmentOnly(libs.spring.boot.devtools)
	developmentOnly(libs.spring.boot.docker.compose)

	// Runtime
	runtimeOnly(libs.jjwt.impl)
	runtimeOnly(libs.jjwt.jackson)
	runtimeOnly(libs.postgresql)

	// Annotation processing
	annotationProcessor(libs.spring.boot.configuration.processor)

	// Test
	testImplementation(libs.spring.boot.starter.data.redis.test)
	testImplementation(libs.spring.boot.starter.flyway.test)
	testImplementation(libs.spring.boot.starter.jooq.test)
	testImplementation(libs.spring.boot.starter.mail.test)
	testImplementation(libs.spring.boot.restdocs)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.spring.boot.testcontainers)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.spring.restdocs.mockmvc)
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
	testImplementation(libs.testcontainers.redis)
	testImplementation(libs.mockito.kotlin)
	testRuntimeOnly(libs.junit.platform.launcher)

	// jOOQ code generation
	jooqCodegen(libs.postgresql)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	outputs.dir(project.extra["snippetsDir"]!!)
}

tasks.asciidoctor {
	inputs.dir(project.extra["snippetsDir"]!!)
	dependsOn(tasks.test)
}


/**
 * Utility function to get environment variables in a safe way.
 *
 * @param name The name of the environment variable.
 * @return The value of the environment variable, or null if it is not set.
 */
fun envOrNull(name: String): String? =
	providers.environmentVariable(name).orNull

flyway {
	url = "${envOrNull("POSTGRES_URL")}/${envOrNull("POSTGRES_DB")}"
	user = envOrNull("POSTGRES_USER")
	password = envOrNull("POSTGRES_PASSWORD")
	locations = arrayOf("filesystem:src/main/resources/db/migration")
	cleanDisabled = true
}

jooq {
	version = libs.versions.jooq.get().toString()

	configurations {
		create("main", Action {
			configuration {
				logging = org.jooq.meta.jaxb.Logging.WARN

				jdbc {
					driver = "org.postgresql.Driver"
					url = "${envOrNull("POSTGRES_URL")}/${envOrNull("POSTGRES_DB")}"
					user = envOrNull("POSTGRES_USER")
					password = envOrNull("POSTGRES_PASSWORD")
				}

				generator {
					name = "org.jooq.codegen.KotlinGenerator"

					database {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "public"
						includes = ".*"
						excludes = "flyway_schema_history|regexp_matches|regexp_split_to_table"

						forcedTypes {
							forcedType {
								userType = "java.lang.String"
								binding = "org.jooq.postgres.extensions.bindings.CitextBinding"
								includeTypes = "citext"
								priority = -2147483648
							}
						}
					}

					generate {
						isRecords = true
						isPojos = true
						isImmutablePojos = true
						isDaos = false
						isFluentSetters = false
						isImplicitJoinPathsToMany = false
						isImplicitJoinPathsManyToMany = false
					}

					target {
						packageName = "dev.haomin.filesheep.jooq"
						directory = "build/generated-src/jooq/main"
					}

					strategy {
						matchers {
							tables {
								table {
									tableIdentifier {
										transform = MatcherTransformType.UPPER
										expression = "$0"
									}

									tableClass {
										transform = MatcherTransformType.PASCAL
										expression = "T__$0"
									}

									recordClass {
										transform = MatcherTransformType.PASCAL
										expression = "R__$0"
									}

									pojoClass {
										transform = MatcherTransformType.PASCAL
										expression = "P__$0"
									}
								}
							}

							fields {
								field {
									fieldIdentifier {
										transform = MatcherTransformType.CAMEL
										expression = "$0"
									}
								}
							}

							enums {
								enum_ {
									enumClass {
										transform = MatcherTransformType.PASCAL
										expression = "E__$0"
									}

									enumLiteral {
										transform = MatcherTransformType.UPPER
										expression = "$0"
									}
								}
							}
						}
					}
				}
			}
		})
	}
}

sourceSets {
	main {
		java {
			srcDir(layout.buildDirectory.dir("generated-src/jooq/main"))
		}
	}
}
