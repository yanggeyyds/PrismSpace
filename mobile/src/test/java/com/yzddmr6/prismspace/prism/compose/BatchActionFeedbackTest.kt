package com.yzddmr6.prismspace.prism.compose

import com.yzddmr6.prismspace.prism.compose.vm.ActionFeedback
import com.yzddmr6.prismspace.prism.compose.vm.BatchAction
import com.yzddmr6.prismspace.prism.compose.vm.batchActionFeedback
import org.junit.Assert.assertEquals
import org.junit.Test

class BatchActionFeedbackTest {

    @Test fun freezeAllSuccess() {
        assertEquals(ActionFeedback("已冻结 3 个应用", false),
            batchActionFeedback(BatchAction.Freeze, 3, 0))
    }
    @Test fun freezePartial() {
        assertEquals(ActionFeedback("冻结完成: 成功 2, 失败 1", true),
            batchActionFeedback(BatchAction.Freeze, 2, 1))
    }
    @Test fun freezeAllFail() {
        assertEquals(ActionFeedback("冻结完成: 成功 0, 失败 3", true),
            batchActionFeedback(BatchAction.Freeze, 0, 3))
    }
    @Test fun uninstallAllSuccess() {
        assertEquals(ActionFeedback("正在卸载 4 个应用", false),
            batchActionFeedback(BatchAction.Uninstall, 4, 0))
    }
    @Test fun uninstallPartial() {
        assertEquals(ActionFeedback("卸载发起: 成功 3, 失败 2", true),
            batchActionFeedback(BatchAction.Uninstall, 3, 2))
    }
    @Test fun uninstallAllFail() {
        assertEquals(ActionFeedback("卸载发起: 成功 0, 失败 1", true),
            batchActionFeedback(BatchAction.Uninstall, 0, 1))
    }
    @Test fun copyAllSuccess() {
        assertEquals(ActionFeedback("正在克隆 5 个应用到双开空间", false),
            batchActionFeedback(BatchAction.CopyToDual, 5, 0))
    }
    @Test fun copyPartial() {
        assertEquals(ActionFeedback("克隆发起: 成功 4, 失败 1", true),
            batchActionFeedback(BatchAction.CopyToDual, 4, 1))
    }
    @Test fun copyAllFail() {
        assertEquals(ActionFeedback("克隆发起: 成功 0, 失败 2", true),
            batchActionFeedback(BatchAction.CopyToDual, 0, 2))
    }
}
