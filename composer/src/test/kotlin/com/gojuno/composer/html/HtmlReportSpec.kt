package com.gojuno.composer.html

import com.gojuno.commander.android.AdbDevice
import com.gojuno.composer.AdbDeviceTest
import com.gojuno.composer.Device
import com.gojuno.composer.Suite
import com.gojuno.composer.perform
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import rx.observers.TestSubscriber
import java.io.File
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class HtmlReportSpec : Spek({

    context("writeHtmlReport") {

        val adbDevice1 = AdbDevice(
                id = "device1",
                online = true
        )

        val suites = listOf(
                Suite(
                        testPackage = "com.gojuno.example1",
                        devices = listOf(Device(id = "device1", logcat = File("device1.logcat"), instrumentationOutput = File("device1.instrumentation"))),
                        tests = listOf(
                                AdbDeviceTest(
                                        adbDevice = adbDevice1,
                                        className = "com.gojuno.example1.TestClass",
                                        testName = "test1",
                                        durationNanos = MILLISECONDS.toNanos(1234),
                                        status = AdbDeviceTest.Status.Passed,
                                        logcat = File("com.gojuno.example1.TestClass", "test1.logcat"),
                                        files = listOf(File("com.gojuno.example1.TestClass.test1", "file1"), File("com.gojuno.example1.TestClass.test1", "file2")),
                                        screenshots = listOf(File("com.gojuno.example1.TestClass.test1", "screenshot1"), File("com.gojuno.example1.TestClass.test1", "screenshot2"))
                                ),
                                AdbDeviceTest(
                                        adbDevice = adbDevice1,
                                        className = "com.gojuno.example1.TestClass",
                                        testName = "test2",
                                        durationNanos = MILLISECONDS.toNanos(1234),
                                        status = AdbDeviceTest.Status.Failed(stacktrace = "abc"),
                                        logcat = File("com.gojuno.example1.TestClass", "test2.logcat"),
                                        files = listOf(File("com.gojuno.example1.TestClass.test2", "file1"), File("com.gojuno.example1.TestClass.test2", "file2")),
                                        screenshots = listOf(File("com.gojuno.example1.TestClass.test2", "screenshot1"), File("com.gojuno.example1.TestClass.test2", "screenshot2"))
                                )
                        ),
                        passedCount = 2,
                        ignoredCount = 0,
                        failedCount = 1,
                        durationNanos = MILLISECONDS.toNanos(1234 * 2),
                        timestampMillis = 1805
                )
        )

        val outputDir by memoized { File("${System.nanoTime()}") }

        val subscriber = TestSubscriber<Unit>()

        fun File.deleteOnExitRecursively() {
            when (isDirectory) {
                false -> deleteOnExit()
                true -> listFiles()?.forEach { inner -> inner.deleteOnExitRecursively()}
            }
        }

        perform {
            writeHtmlReport(Gson(), suites, outputDir).subscribe(subscriber)
            subscriber.awaitTerminalEvent(5, SECONDS)
            outputDir.deleteOnExitRecursively()
        }

        it("completes") {
            subscriber.assertCompleted()
        }

        it("does not emit error") {
            subscriber.assertNoErrors()
        }

        it("creates index.json") {
            assertThat(File(outputDir, "index.json").readText()).isEqualTo(
                    """{"suites":[{"id":"0","passed_count":2,"ignored_count":0,"failed_count":1,"duration_millis":2468,"devices":[{"id":"device1","logcat_path":"device1.logcat","instrumentation_output_path":"device1.instrumentation"}]}]}"""
            )
        }

        it("creates suite json") {
            assertThat(File(File(outputDir, "suites"), "0.json").readText()).isEqualTo(
                    """{"id":"0","tests":[{"id":"com.gojuno.example1TestClasstest1","package_name":"com.gojuno.example1","class_name":"TestClass","name":"test1","duration_millis":1234,"status":"passed","deviceId":"device1","properties":{}},{"id":"com.gojuno.example1TestClasstest2","package_name":"com.gojuno.example1","class_name":"TestClass","name":"test2","duration_millis":1234,"status":"failed","deviceId":"device1","properties":{}}],"passed_count":2,"ignored_count":0,"failed_count":1,"duration_millis":2468,"devices":[{"id":"device1","logcat_path":"device1.logcat","instrumentation_output_path":"device1.instrumentation"}]}"""
            )
        }

        it("creates json for 1st test") {
            assertThat(File(File(File(File(outputDir, "suites"), "0"), "device1"), "com.gojuno.example1TestClasstest1.json").readText()).isEqualTo(
                    """{"package_name":"com.gojuno.example1","class_name":"TestClass","name":"test1","id":"com.gojuno.example1TestClasstest1","duration_millis":1234,"status":"passed","logcat_path":"com.gojuno.example1.TestClass/test1.logcat","deviceId":"device1","properties":{},"file_paths":["com.gojuno.example1.TestClass.test1/file1","com.gojuno.example1.TestClass.test1/file2"],"screenshots_paths":["com.gojuno.example1.TestClass.test1/screenshot1","com.gojuno.example1.TestClass.test1/screenshot2"]}"""
            )
        }

        it("creates json for 2nd test") {
            assertThat(File(File(File(File(outputDir, "suites"), "0"), "device1"), "com.gojuno.example1TestClasstest2.json").readText()).isEqualTo(
                    """{"package_name":"com.gojuno.example1","class_name":"TestClass","name":"test2","id":"com.gojuno.example1TestClasstest2","duration_millis":1234,"status":"failed","stacktrace":"abc","logcat_path":"com.gojuno.example1.TestClass/test2.logcat","deviceId":"device1","properties":{},"file_paths":["com.gojuno.example1.TestClass.test2/file1","com.gojuno.example1.TestClass.test2/file2"],"screenshots_paths":["com.gojuno.example1.TestClass.test2/screenshot1","com.gojuno.example1.TestClass.test2/screenshot2"]}"""
            )
        }
    }
})
