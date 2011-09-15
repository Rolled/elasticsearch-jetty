package org.elasticsearch.http.jetty;

import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author imotov
 */
public class JettyHttpServerRestChannel implements HttpChannel {
    private final RestRequest restRequest;

    private final HttpServletResponse resp;

    private IOException sendFailure;

    private final CountDownLatch latch;

    JettyHttpServerRestChannel(RestRequest restRequest, HttpServletResponse resp) {
        this.restRequest = restRequest;
        this.resp = resp;
        this.latch = new CountDownLatch(1);
    }

    public void await() throws InterruptedException {
        latch.await();
    }

    public IOException sendFailure() {
        return sendFailure;
    }

    @Override
    public void sendResponse(RestResponse response) {
        resp.setContentType(response.contentType());
        resp.addHeader("Access-Control-Allow-Origin", "*");
        if (response.status() != null) {
            resp.setStatus(response.status().getStatus());
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        if (restRequest.method() == RestRequest.Method.OPTIONS) {
            // also add more access control parameters
            resp.addHeader("Access-Control-Max-Age", "1728000");
            resp.addHeader("Access-Control-Allow-Methods", "PUT, DELETE");
            resp.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
        }
        try {
            int contentLength = response.contentLength() + response.prefixContentLength() + response.suffixContentLength();
            resp.setContentLength(contentLength);
            ServletOutputStream out = resp.getOutputStream();
            if (response.prefixContent() != null) {
                out.write(response.prefixContent(), 0, response.prefixContentLength());
            }
            out.write(response.content(), 0, response.contentLength());
            if (response.suffixContent() != null) {
                out.write(response.suffixContent(), 0, response.suffixContentLength());
            }
            out.close();
        } catch (IOException e) {
            sendFailure = e;
        } finally {
            latch.countDown();
        }
     }
}