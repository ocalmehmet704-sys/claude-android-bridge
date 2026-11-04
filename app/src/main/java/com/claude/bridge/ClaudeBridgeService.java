package com.claude.bridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClaudeBridgeService extends AccessibilityService {

    private static final String OUT_DIR = "/storage/emulated/0/claude.code/bridge/";
    private static final String NOTIF_LOG = OUT_DIR + "notifications.jsonl";
    private static final String EVENT_LOG = OUT_DIR + "events.jsonl";

    @Override
    public void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);

        new File(OUT_DIR).mkdirs();
        writeLog(OUT_DIR + "status.txt", "RUNNING " + now());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            int type = event.getEventType();
            String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String text = event.getText() != null ? event.getText().toString() : "";

            JSONObject j = new JSONObject();
            j.put("time", now());
            j.put("pkg", pkg);
            j.put("text", text);

            if (type == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                Parcelable p = event.getParcelableData();
                if (p instanceof Notification) {
                    Notification n = (Notification) p;
                    Bundle extras = n.extras;
                    if (extras != null) {
                        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
                        CharSequence body  = extras.getCharSequence(Notification.EXTRA_TEXT);
                        j.put("title", title != null ? title.toString() : "");
                        j.put("body",  body  != null ? body.toString()  : "");
                    }
                }
                j.put("type", "notification");
                appendLine(NOTIF_LOG, j.toString());
            } else {
                j.put("type", "event");
                appendLine(EVENT_LOG, j.toString());
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onInterrupt() {}

    private void appendLine(String path, String line) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, true))) {
            pw.println(line);
        } catch (Exception ignored) {}
    }

    private void writeLog(String path, String content) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path, false))) {
            pw.print(content);
        } catch (Exception ignored) {}
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
