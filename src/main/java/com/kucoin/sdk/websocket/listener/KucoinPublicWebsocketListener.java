/**
 * Copyright 2019 Mek Global Limited.
 */
package com.kucoin.sdk.websocket.listener;

import static com.kucoin.sdk.constants.APIConstants.API_LEVEL2_TOPIC_PREFIX;
import static com.kucoin.sdk.constants.APIConstants.API_LEVEL3_TOPIC_PREFIX;
import static com.kucoin.sdk.constants.APIConstants.API_MATCH_TOPIC_PREFIX;
import static com.kucoin.sdk.constants.APIConstants.API_TICKER_TOPIC_PREFIX;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kucoin.sdk.exception.KucoinApiException;
import com.kucoin.sdk.rest.response.TickerResponse;
import com.kucoin.sdk.websocket.KucoinAPICallback;
import com.kucoin.sdk.websocket.PrintCallback;
import com.kucoin.sdk.websocket.event.KucoinEvent;
import com.kucoin.sdk.websocket.event.Level2ChangeEvent;
import com.kucoin.sdk.websocket.event.Level3ChangeEvent;
import com.kucoin.sdk.websocket.event.MatchExcutionChangeEvent;
import com.kucoin.sdk.websocket.event.TickerChangeEvent;

import lombok.Data;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Created by chenshiwei on 2019/1/10.
 */
@Data
public class KucoinPublicWebsocketListener extends WebSocketListener {

    private static final ObjectMapper OBJECTMAPPER = new ObjectMapper();
    {
        OBJECTMAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private KucoinAPICallback<KucoinEvent<TickerChangeEvent>> tickerCallback = new PrintCallback();
    private KucoinAPICallback<KucoinEvent<Level2ChangeEvent>> level2Callback = new PrintCallback();
    private KucoinAPICallback<KucoinEvent<MatchExcutionChangeEvent>> matchDataCallback = new PrintCallback();
    private KucoinAPICallback<KucoinEvent<Level3ChangeEvent>> level3Callback = new PrintCallback();

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("web socket open");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      JsonNode jsonObject = tree(text);
        String type = jsonObject.get("type").textValue();
        String topic = jsonObject.get("topic").textValue();

        if (!type.equals("message")) {
            System.out.println(text);
        } else {
            if (topic.contains(API_TICKER_TOPIC_PREFIX)) {
                KucoinEvent kucoinEvent = deserialize(text, new TypeReference<KucoinEvent<TickerResponse>>() {});
                tickerCallback.onResponse(kucoinEvent);
            } else if (topic.contains(API_LEVEL2_TOPIC_PREFIX)) {
                KucoinEvent kucoinEvent = deserialize(text, new TypeReference<KucoinEvent<Level2ChangeEvent>>() {});
                level2Callback.onResponse(kucoinEvent);
            } else if (topic.contains(API_MATCH_TOPIC_PREFIX)) {
                KucoinEvent kucoinEvent = deserialize(text, new TypeReference<KucoinEvent<MatchExcutionChangeEvent>>() {});
                matchDataCallback.onResponse(kucoinEvent);
            } else if (topic.contains(API_LEVEL3_TOPIC_PREFIX)) {
                KucoinEvent kucoinEvent = deserialize(text, new TypeReference<KucoinEvent<Level3ChangeEvent>>() {});
                level3Callback.onResponse(kucoinEvent);
            }
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        throw new KucoinApiException(t.getMessage());
    }

    private JsonNode tree(String text) {
      try {
        return OBJECTMAPPER.readTree(text);
      } catch (IOException e) {
        throw new RuntimeException("Failed to deserialise message: " + text, e);
      }
    }

    private <T> T deserialize(String text, TypeReference<T> typeReference) {
      try {
        return OBJECTMAPPER.readValue(text, typeReference);
      } catch (IOException e) {
        throw new RuntimeException("Failed to deserialise message: " + text, e);
      }
    }
}
