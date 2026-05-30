package com.yzddmr6.prismspace.model

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yzddmr6.prismspace.common.app.BaseAndroidViewModel
import com.yzddmr6.prismspace.analytics.Analytics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

fun AndroidViewModel.interactive(context: Context, block: suspend CoroutineScope.() -> Unit) {
	viewModelScope.launch(CoroutineExceptionHandler { _, e -> handleException(context, "Prism.VM", e) }, block = block)
}

fun <R> BaseAndroidViewModel.interactiveFuture(context: Context, block: suspend CoroutineScope.() -> R): CompletableFuture<R?> {
	return viewModelScope.future(block = block).exceptionally { t -> null.also { handleException(context, tag, t) }}
}

private fun handleException(context: Context, tag: String, t: Throwable) {
	if (t is CancellationException) return Unit.also { Log.i(tag, "Interaction canceled: ${t.message}") }
	Analytics().logAndReport(tag, "Unexpected internal error", t)
	Toast.makeText(context, "Internal error: " + t.message, Toast.LENGTH_LONG).show()
}
