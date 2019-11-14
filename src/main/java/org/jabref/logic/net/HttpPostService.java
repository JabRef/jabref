package org.jabref.logic.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.jabref.JabRefExecutorService;

/**
 * Makes it easy to send http post requests
 * HttpClients are reused.
 */
public class HttpPostService {

    private static final HttpClient HTTP_CLIENT = HttpClients.createDefault();
    private String url; //The root URL

    public HttpPostService(String url) {
        this.url = url;
    }

    public HttpResponse sendPostAndWait(Map<String, String> params) throws HttpPostServiceException {
        try {
            return HTTP_CLIENT.execute(prepareHttpPost(params));
        } catch (IOException e) {
            throw new HttpPostServiceException();
        }
    }

    public Future<HttpResponse> sendPost(String subUrl,
                                         Map<String, String> params,
                                         FutureCallback<HttpResponse> httpResponseHandler) throws IOException {
        return JabRefExecutorService.INSTANCE.execute(() -> HTTP_CLIENT.execute(prepareHttpPost(params, subUrl)));
        return httpAsyncClient.execute(prepareHttpPost(params, subUrl), httpResponseHandler);
    }

    public Future<HttpResponse> sendPost(Map<String, String> params) throws HttpPostServiceException {
        return JabRefExecutorService.INSTANCE.execute(new Callable<HttpResponse>() {
          @Override
          public HttpResponse call() throws HttpPostServiceException {
            try {
              return HTTP_CLIENT.execute(prepareHttpPost(params));
            } catch (IOException e) {
              throw new HttpPostServiceException();
            }
          }
        });
    }

    private HttpPost prepareHttpPost(Map<String, String> params, String urlSubPath) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url + urlSubPath);
        List<BasicNameValuePair> parameterList = new ArrayList<>();
        for (Map.Entry<String, String> e: params.entrySet()) {
            parameterList.add(new BasicNameValuePair(e.getKey(), e.getValue()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(parameterList, "UTF-8"));
        return httpPost;
    }

    private HttpPost prepareHttpPost(Map<String, String> params) throws UnsupportedEncodingException {
      return prepareHttpPost(params, "");
    }



}
