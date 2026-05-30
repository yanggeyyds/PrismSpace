package com.yzddmr6.prismspace.controller

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ShizukuLifecycleGuardTest {

    @Test fun cloneWorkerRemovesOneShotShizukuUserServiceAfterUse() {
        val source = String(
            Files.readAllBytes(findProjectRoot().resolve("mobile/src/main/java/com/yzddmr6/prismspace/controller/PrismAppClones.kt")),
            StandardCharsets.UTF_8
        )

        assertTrue(
            "Shizuku clone worker must remove the one-shot UserService after transact/timeout.",
            source.contains("Shizuku.unbindUserService(args, conn, true)")
        )
        assertFalse(
            "Do not pass remove=false for clone UserService cleanup; it leaves root worker processes behind.",
            source.contains("Shizuku.unbindUserService(args, conn, false)")
        )
        assertTrue(
            "Each one-shot clone bind should use a unique Shizuku tag so repeated clone attempts do not reuse a stale default service identity.",
            source.contains(".tag(shizukuServiceTag)")
        )
    }

    @Test fun privilegedWorkerExitsWhenShizukuRemovesUserService() {
        val source = String(
            Files.readAllBytes(findProjectRoot().resolve("mobile/src/main/java/com/yzddmr6/prismspace/controller/PrivilegedRemoteWorker.kt")),
            StandardCharsets.UTF_8
        )

        assertTrue(
            "Shizuku remove=true sends a destroy transaction; the privileged worker must handle it.",
            source.contains("TRANSACTION_DESTROY")
        )
        assertTrue(
            "The destroy transaction must schedule process exit after Shizuku receives the binder reply.",
            source.contains("scheduleProcessExit()")
        )
    }

    private fun findProjectRoot(): Path {
        var current = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        while (true) {
            if (Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("mobile/build.gradle"))) {
                return current
            }
            current = current.parent ?: error("Cannot locate PrismSpace project root from ${System.getProperty("user.dir")}")
        }
    }
}
