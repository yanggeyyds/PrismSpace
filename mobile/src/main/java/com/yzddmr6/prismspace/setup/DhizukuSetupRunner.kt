package com.yzddmr6.prismspace.setup

import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import android.os.Parcel
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuUserServiceArgs
import com.yzddmr6.prismspace.analytics.DiagnosticLog
import com.yzddmr6.prismspace.controller.PrivilegedRemoteWorker
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object DhizukuSetupRunner {
    private const val TAG = "Prism.DhizukuSetup"
    private const val TIMEOUT_SECONDS = 90L

    /** Error: Dhizuku.init() returned false — Dhizuku not installed or not activated. */
    const val ERR_INIT = -2
    /** Error: bindUserService returned false — Dhizuku server not running or permission denied. */
    const val ERR_BIND = -3
    /** Error: Binder transaction timed out — service connected but no reply within [TIMEOUT_SECONDS]s. */
    const val ERR_TIMEOUT = -4
    /** Error: Binder transaction threw an exception (logged separately). */
    const val ERR_TRANSACT = -5
    /** Error: Service disconnected before transaction completed. */
    const val ERR_DISCONNECT = -6

    fun execSetup(context: Context, adminFlat: String, parentUserId: Int, enginePkg: String): Int {
        DiagnosticLog.i(TAG, "execSetup admin=$adminFlat parent=$parentUserId engine=$enginePkg")

        if (!Dhizuku.init(context.applicationContext)) {
            DiagnosticLog.e(TAG, "Dhizuku.init returned false — Dhizuku not installed or not activated", null)
            return ERR_INIT
        }

        val component = ComponentName(context, PrivilegedRemoteWorker::class.java)
        val args = DhizukuUserServiceArgs(component)
        val output = AtomicInteger(-1)
        val latch = CountDownLatch(1)
        lateinit var conn: android.content.ServiceConnection

        conn = object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                DiagnosticLog.i(TAG, "Dhizuku service connected name=$name")
                Thread({
                    try {
                        val data = Parcel.obtain()
                        val reply = Parcel.obtain()
                        data.writeString(adminFlat)
                        data.writeInt(parentUserId)
                        data.writeString(enginePkg)
                        service.transact(PrivilegedRemoteWorker.TRANSACTION_SETUP_PROFILE, data, reply, 0)
                        val result = reply.readInt()
                        DiagnosticLog.i(TAG, "setupManagedProfile returned userId=$result")
                        output.set(result)
                    } catch (e: Exception) {
                        DiagnosticLog.e(TAG, "Dhizuku transact failed", e)
                        output.set(ERR_TRANSACT)
                    } finally {
                        latch.countDown()
                        try { Dhizuku.unbindUserService(conn) } catch (_: Exception) {}
                    }
                }, "DhizukuSetup").start()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                DiagnosticLog.e(TAG, "Dhizuku service disconnected unexpectedly name=$name", null)
                output.compareAndSet(-1, ERR_DISCONNECT)
                latch.countDown()
            }
        }

        if (!Dhizuku.bindUserService(args, conn)) {
            DiagnosticLog.e(TAG, "Dhizuku.bindUserService returned false — is Dhizuku server running?", null)
            return ERR_BIND
        }

        if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            DiagnosticLog.e(TAG, "Dhizuku setup timed out after ${TIMEOUT_SECONDS}s", null)
            try { Dhizuku.unbindUserService(conn) } catch (_: Exception) {}
            return ERR_TIMEOUT
        }

        return output.get()
    }
}