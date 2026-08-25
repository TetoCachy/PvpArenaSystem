package com.tetocachy.pvparenasystem.client.data;

import com.tetocachy.pvparenasystem.network.S2CSyncArenaDataPayload;

public class ClientArenaCache {
    public static S2CSyncArenaDataPayload currentData = null;

    public static void update(S2CSyncArenaDataPayload data) {
        currentData = data;
    }

    public static boolean hasData() {
        return currentData != null;
    }
}