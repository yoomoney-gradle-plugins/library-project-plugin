package ru.yandex.money.gradle.plugins.library

import ru.yandex.money.gradle.plugins.library.dependencies.CheckDependenciesPlugin

/**
 * @author Oleg Kandaurov
 * @since 15.11.2018
 */
class LibraryPluginSpec extends AbstractPluginSpec {

    def "should contains tasks"() {
        def expectedTasks = [CheckDependenciesPlugin.CHECK_DEPENDENCIES_TASK_NAME,
                             "preRelease", "release", "checkChangelog"]

        when:
        def result = runTasksSuccessfully("tasks")

        then:
        expectedTasks.forEach({
            assert result.standardOutput.contains(it)
        })
    }

    def "should complete simple java build"() {
        when:
        writeHelloWorld("ru.yandex.money.common")
        then:
        runTasksSuccessfully("clean", "build", "slowTest")

    }

    def "should run java test"() {

        given: "Java тест в resources/src/test/java"
        directory("src/test/java")
        copyResources("classes", "src/test/java")

        when:
        def result = runTasks("clean", "build")

        then: "Запускаются тесты на Java"

        assert result.success
        assert result.standardOutput.contains("run java test...")

    }

    def "should run kotlin test"() {

        given: "Kotlin тест в resources/src/test/java"
        directory("src/test/kotlin")
        copyResources("classes", "src/test/kotlin")

        when:
        def result = runTasks("clean", "build")

        then: "Запускаются тесты на Kotlin"
        assert result.success
        assert result.standardOutput.contains("run kotlin test...")
    }

    def "should check snapshot dependencies"() {
        when:
        def result = runTasks("checkComponentSnapshotDependencies")

        then: "Запускается проверка на SNAPSHOT зависимости"
        assert result.success
        assert result.standardOutput.contains("checkLibraryDependencies")
    }

}
