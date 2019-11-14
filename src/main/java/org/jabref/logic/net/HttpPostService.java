package org.jabref.logic.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.http.nio.client.HttpAsyncClient;

/**
 * Makes it easy to send http post requests
 * HttpClients are reused.
 */
public class HttpPostService {

    private static final HttpClient HTTP_CLIENT = HttpClients.createDefault();
    private static final HttpAsyncClient ASYNC_HTTP_CLIENT = HttpAsyncClients.createDefault();
    private String url; //The root URL

    public HttpPostService(String url) {
        this.url = url;
    }

    public HttpResponse sendPostAndWait(String subUrl, Map<String, String> params) throws HttpPostServiceException {
        try {
            return HTTP_CLIENT.execute(prepareHttpPost(params, subUrl));
        } catch (IOException e) {
            throw new HttpPostServiceException();
        }
    }

    public HttpResponse sendPostAndWait(Map<String, String> params) throws HttpPostServiceException {
        return sendPostAndWait("", params);
    }

    public Future<HttpResponse> sendPost(String subUrl,
                                         Map<String, String> params,
                                         FutureCallback<HttpResponse> httpResponseHandler) throws HttpPostServiceException {
        try {
            return ASYNC_HTTP_CLIENT.execute(prepareHttpPost(params, subUrl), httpResponseHandler);
        } catch (IOException e) {
            throw new HttpPostServiceException();
        }
    }

    public Future<HttpResponse> sendPost(String subUrl,
                                         Map<String, String> params) throws HttpPostServiceException {
        return sendPost(subUrl, params, null);

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
