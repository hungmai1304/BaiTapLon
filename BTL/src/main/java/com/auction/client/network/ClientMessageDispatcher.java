package com.auction.client.network;

import com.auction.protocol.Response;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientMessageDispatcher {

    private static final Gson gson =
            new Gson();

    // type -> listeners
    private static final Map<String,
            List<MessageListener>> listeners
            = new HashMap<>();

    private ClientMessageDispatcher() {
    }

    public static void register(String type,
                                MessageListener listener) {

        listeners
                .computeIfAbsent(
                        type,
                        k -> new ArrayList<>()
                )
                .add(listener);
    }

    public static void unregister(String type,
                                  MessageListener listener) {

        List<MessageListener> listenerList =
                listeners.get(type);

        if (listenerList != null) {

            listenerList.remove(listener);

            if (listenerList.isEmpty()) {

                listeners.remove(type);
            }
        }
    }

    public static void dispatch(String jsonMessage) {

        try {

            Response response =
                    gson.fromJson(
                            jsonMessage,
                            Response.class
                    );

            String type = response.getType();

            List<MessageListener> listenerList =
                    listeners.get(type);

            if (listenerList != null) {

                List<MessageListener> copy =
                        new ArrayList<>(listenerList);

                for (MessageListener listener : copy) {

                    listener.onMessageReceived(
                            jsonMessage
                    );
                }

            } else {

                System.out.println(
                        "⚠ Không có listener cho type: "
                                + type
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "❌ Lỗi parse JSON:"
            );

            e.printStackTrace();
        }
    }
}