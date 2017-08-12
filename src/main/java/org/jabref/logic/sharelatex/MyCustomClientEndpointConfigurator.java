package org.jabref.logic.sharelatex;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.HandshakeResponse;

public class MyCustomClientEndpointConfigurator extends ClientEndpointConfig.Configurator {

    private final String userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:53.0) Gecko/20100101 Firefox/53.0";
    private final String serverOrigin;
    private final Map<String, String> cookies;

    public MyCustomClientEndpointConfigurator(String serverOrigin, Map<String, String> cookies) {
        super();
        this.serverOrigin = serverOrigin;
        this.cookies = cookies;
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {

        headers.put("User-Agent", Arrays.asList(userAgent));
        headers.put("Origin", Arrays.asList(serverOrigin));

        String result = cookies.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; "));
        headers.put("Cookie", Arrays.asList(result));
    }

    @Override
    public void afterResponse(HandshakeResponse handshakeResponse) {
        final Map<String, List<String>> headers = handshakeResponse.getHeaders();

        System.out.println("headers " + headers);

    }
}
