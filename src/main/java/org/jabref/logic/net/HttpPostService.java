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
import java.util.concurrent.Future;


/**
 * Makes it easy to send http post requests
 * HttpClients are reused.
 */
public class HttpPostService {

    private static final HttpClient HTTP_CLIENT = HttpClients.createSystem();

    public static HttpResponse sendPostAndWait(String url, String subUrl, Map<String, String> params) throws HttpPostServiceException {
        try {
            return HTTP_CLIENT.execute(prepareHttpPost(url, subUrl, params));
        } catch (IOException e) {
            throw new HttpPostServiceException();
        }
    }

    public static HttpResponse sendPostAndWait(String url, Map<String, String> params) throws HttpPostServiceException {
        return sendPostAndWait(url, "", params);
    }

    private static HttpPost prepareHttpPost(String url, String urlSubPath, Map<String, String> params) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url + urlSubPath);
        List<BasicNameValuePair> parameterList = new ArrayList<>();
        for (Map.Entry<String, String> e: params.entrySet()) {
            parameterList.add(new BasicNameValuePair(e.getKey(), e.getValue()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(parameterList, "UTF-8"));
        return httpPost;
    }

    private static HttpPost prepareHttpPost(String url, Map<String, String> params) throws UnsupportedEncodingException {
      return prepareHttpPost(url, "", params);
    }


}
