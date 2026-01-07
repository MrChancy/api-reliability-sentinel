package com.fluffycat.sentinelapp.common.interceptor;

import com.fluffycat.sentinelapp.common.constants.ConstantText;
import com.fluffycat.sentinelapp.common.trace.TraceIdUtil;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import java.io.IOException;

public class TraceIdInterceptor implements Interceptor {
    @Override
    public @NonNull Response intercept(Interceptor.@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String traceId = MDC.get(TraceIdUtil.TRACE_ID);
        if (traceId != null) {
            Request newRequest = originalRequest.newBuilder()
                    .header(ConstantText.TRACE_HEADER, traceId)
                    .build();
            return chain.proceed(newRequest);
        }
        return chain.proceed(originalRequest);
    }
}
