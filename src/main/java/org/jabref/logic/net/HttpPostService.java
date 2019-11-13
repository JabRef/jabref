package org.jabref.logic.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
//import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.HttpClients;
//import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.nio.client.HttpAsyncClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//import java.util.concurrent.Future;

/**
 * Makes it easy to send http post requests
 * HttpClients are reused.
 */
public class HttpPostService {

    private static final HttpClient httpClient = HttpClients.createDefault();
    //private static final HttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault();
    private URI url;

    public HttpPostService(String url) throws URISyntaxException {
        this(new URI(url));
    }

    public HttpPostService(URI url) {
        this.url = url;
    }

    public HttpResponse sendPostAndWait(Map<String, String> params) throws IOException {
        return httpClient.execute(prepareHttpPost(params));
    }

    /* sadly this does not work due to gradle compile errors
    public Future<HttpResponse> sendPost(Map<String, String> params,
                                         FutureCallback<HttpResponse> httpResponseHandler) throws IOException {
        return httpAsyncClient.execute(prepareHttpPost(params), httpResponseHandler);
    }

    public Future<HttpResponse> sendPost(Map<String, String> params) throws IOException {
        return httpAsyncClient.execute(prepareHttpPost(params), null);
    }*/

    private HttpPost prepareHttpPost(Map<String, String> params) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        List<BasicNameValuePair> parameterList = new ArrayList<>();
        for (Map.Entry<String, String> e: params.entrySet()) {
            parameterList.add(new BasicNameValuePair(e.getKey(), e.getValue()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(parameterList, "UTF-8"));
        return httpPost;
    }

}
