package com.yzddmr6.prismspace.prism.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class FeedbackContactResourcesTest {

    @Test
    fun `feedback contact strings exist for supported locales`() {
        val root = findProjectRoot()
        val expected = listOf(
            ExpectedStrings(
                path = "mobile/src/main/res/values/strings_set.xml",
                title = "Feedback and discussion",
                summary = "For feedback and discussion, search WeChat for “%1\$s”.",
                copy = "Copy",
                copied = "WeChat account name copied",
            ),
            ExpectedStrings(
                path = "mobile/src/main/res/values-zh/strings_set.xml",
                title = "反馈与交流",
                summary = "有问题反馈或交流讨论，微信搜索「%1\$s」。",
                copy = "复制",
                copied = "已复制公众号名称",
            ),
            ExpectedStrings(
                path = "mobile/src/main/res/values-zh-rTW/strings_set.xml",
                title = "回饋與交流",
                summary = "有問題回饋或交流討論，微信搜尋「%1\$s」。",
                copy = "複製",
                copied = "已複製公眾號名稱",
            ),
        )

        expected.forEach { item ->
            val strings = readStrings(root.resolve(item.path))
            assertEquals("feedback title in ${item.path}", item.title, strings["lz_set_feedback_title"])
            assertEquals("feedback summary in ${item.path}", item.summary, strings["lz_set_feedback_summary"])
            assertEquals("feedback copy label in ${item.path}", item.copy, strings["lz_set_feedback_copy"])
            assertEquals("feedback copied message in ${item.path}", item.copied, strings["lz_set_feedback_copied"])
            assertEquals("feedback account in ${item.path}", "熵减矩阵", strings["lz_set_feedback_account"])
            assertTrue("summary should format account placeholder in ${item.path}", item.summary.contains("%1\$s"))
        }
    }

    private data class ExpectedStrings(
        val path: String,
        val title: String,
        val summary: String,
        val copy: String,
        val copied: String,
    )

    private fun readStrings(path: Path): Map<String, String> {
        assertTrue("resource file exists: $path", Files.exists(path))
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(path.toFile())
        val nodes = doc.getElementsByTagName("string")
        return buildMap {
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as Element
                put(element.getAttribute("name"), element.textContent)
            }
        }
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
