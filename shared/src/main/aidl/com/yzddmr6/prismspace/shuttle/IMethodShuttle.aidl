package com.yzddmr6.prismspace.shuttle;

import com.yzddmr6.prismspace.shuttle.MethodInvocation;

interface IMethodShuttle {
    void invoke(inout MethodInvocation invocation);
}
