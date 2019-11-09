package org.jabref.logic.plaintextparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class GrobidRequester {

  //private final HttpClient

  //Sample code from https://stackoverflow.com/questions/3324717/sending-http-post-request-in-java (edited)
  public String sendReference(String plainText) {
    try {
      HttpClient httpclient = HttpClients.createDefault();
      HttpPost httppost = new HttpPost("http://localhost:8070/api/processCitation");

// Request parameters and other properties.
      List<NameValuePair> params = new ArrayList<>(2);
      params.add(new BasicNameValuePair("citations", plainText));
      params.add(new BasicNameValuePair("consolidateCitations", "1"));
      httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

//Execute and get the response.
        System.out.println("now requesting");
      HttpResponse response = httpclient.execute(httppost);
        System.out.println("Getting entity");
      HttpEntity entity = response.getEntity();
      System.out.println("Done");

      String result = "Fail";
      if (entity != null) {
        try (InputStream instream = entity.getContent()) {
          Scanner s = new Scanner(instream).useDelimiter("\\A");
          result = s.hasNext() ? s.next() : "";
        }
      }
      return result;
    } catch (IOException e) {
      System.out.println("Something went wrong");
      System.out.println(e.getMessage());
    }
    return "fail";
  }

}
