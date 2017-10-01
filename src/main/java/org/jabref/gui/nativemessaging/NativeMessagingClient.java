package org.jabref.gui.nativemessaging;

import java.util.concurrent.Future;

import org.json.JSONObject;

public interface NativeMessagingClient {

    Future<JSONObject> sendAsync(String message);
}
