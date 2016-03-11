package com.bendaschel.lab9;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AwaitEvents {

    public interface Callback {
        /**
         * Called when all requested events have been fired.
         */
        void onReady();
    }

    private final HashMap<String, Boolean> mRegisteredEvents;
    private Callback mCallback;

    public AwaitEvents() {
        mRegisteredEvents = new HashMap<String, Boolean>();
    }

    public void fireEvent(String event) {
        synchronized (mRegisteredEvents) {
            mRegisteredEvents.put(event, true);
            Set<Map.Entry<String, Boolean>> entries = mRegisteredEvents.entrySet();
            boolean isReady = true;
            for (Map.Entry<String, Boolean> entry: entries) {
                isReady &= entry.getValue();
            }
            if (isReady) {
                notifyListener();
            }
        }
    }

    private void notifyListener() {
        if (mCallback != null) {
            mCallback.onReady();
        }
    }

    public AwaitEvents awaitEvent(String event) {
        synchronized (mRegisteredEvents) {
            mRegisteredEvents.put(event, false);
        }
        return this;
    }

    public AwaitEvents setListener(Callback readyCallback) {
        mCallback = readyCallback;
        return this;
    }
}
