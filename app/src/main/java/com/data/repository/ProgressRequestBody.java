package com.data.repository;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
import androidx.annotation.NonNull;

public class ProgressRequestBody extends RequestBody {

    private final RequestBody requestBody;
    private final ProgressListener listener;

    public interface ProgressListener {
        void onProgressUpdate(long bytesWritten, long contentLength);
    }

    public ProgressRequestBody(RequestBody requestBody, ProgressListener listener) {
        this.requestBody = requestBody;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        BufferedSink bufferedSink = Okio.buffer(new CountingSink(sink));
        requestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    protected final class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;

        CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            if (listener != null) {
                listener.onProgressUpdate(bytesWritten, contentLength());
            }
        }
    }
}