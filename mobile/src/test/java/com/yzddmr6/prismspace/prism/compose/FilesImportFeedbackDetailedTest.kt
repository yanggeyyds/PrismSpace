package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.ActionFeedback
import com.yzddmr6.prismspace.prism.compose.vm.filesImportFeedback
import com.yzddmr6.prismspace.prism.compose.vm.filesImportFeedbackDetailed
import org.junit.Assert.assertEquals
import org.junit.Test

class FilesImportFeedbackDetailedTest {

    @Test fun allZeroIsEmptyNonError() {
        assertEquals(ActionFeedback("", false), filesImportFeedbackDetailed(0, 0, 0))
    }

    @Test fun noOversizeDelegatesVerbatimToExistingMapper() {
        assertEquals(filesImportFeedback(5, 0), filesImportFeedbackDetailed(5, 0, 0))
        assertEquals(filesImportFeedback(0, 2), filesImportFeedbackDetailed(0, 0, 2))
        assertEquals(filesImportFeedback(3, 2), filesImportFeedbackDetailed(3, 0, 2))
    }

    @Test fun noOversizeExactStrings() {
        assertEquals(ActionFeedback("已导入 3 个文件", false), filesImportFeedbackDetailed(3, 0, 0))
        assertEquals(ActionFeedback("导入失败：2 个文件未能导入", true), filesImportFeedbackDetailed(0, 0, 2))
        assertEquals(ActionFeedback("导入完成：成功 3，失败 2", true), filesImportFeedbackDetailed(3, 0, 2))
    }

    @Test fun oversizeOnly() {
        assertEquals(ActionFeedback("导入完成：成功 0，超限跳过 2，失败 0", true),
            filesImportFeedbackDetailed(0, 2, 0))
    }

    @Test fun oversizeWithSuccess() {
        assertEquals(ActionFeedback("导入完成：成功 3，超限跳过 1，失败 0", true),
            filesImportFeedbackDetailed(3, 1, 0))
    }

    @Test fun oversizeWithSuccessAndOtherFail() {
        assertEquals(ActionFeedback("导入完成：成功 3，超限跳过 1，失败 2", true),
            filesImportFeedbackDetailed(3, 1, 2))
    }
}
