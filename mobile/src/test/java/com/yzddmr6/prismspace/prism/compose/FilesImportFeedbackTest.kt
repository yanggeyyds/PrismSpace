package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.ActionFeedback
import com.yzddmr6.prismspace.prism.compose.vm.filesImportFeedback
import org.junit.Assert.assertEquals
import org.junit.Test

class FilesImportFeedbackTest {
    @Test fun noneAttempted_isSilent() {
        assertEquals(ActionFeedback("", false), filesImportFeedback(0, 0))
    }
    @Test fun allOk() {
        assertEquals(ActionFeedback("已导入 3 个文件", false), filesImportFeedback(3, 0))
    }
    @Test fun partial_isError() {
        assertEquals(ActionFeedback("导入完成：成功 2，失败 1", true), filesImportFeedback(2, 1))
    }
    @Test fun allFail_isError() {
        assertEquals(ActionFeedback("导入失败：4 个文件未能导入", true), filesImportFeedback(0, 4))
    }
}
