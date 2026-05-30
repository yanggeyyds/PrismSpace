package com.yzddmr6.prismspace.shuttle;

import android.content.ComponentName;
import com.yzddmr6.prismspace.shuttle.IUnbinder;

interface IServiceConnection {
    oneway void onServiceConnected(in ComponentName name, in IBinder service, in IUnbinder unbinder);
    oneway void onServiceDisconnected(in ComponentName name);
    oneway void onServiceFailed();
}
