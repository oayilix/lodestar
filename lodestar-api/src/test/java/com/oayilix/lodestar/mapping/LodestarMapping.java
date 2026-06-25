package com.oayilix.lodestar.mapping;

import android.app.Activity;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LodestarMapping {
    private LodestarMapping() {}

    public static Map<String, Class<? extends Activity>> get() {
        Map<String, Class<? extends Activity>> routes = new LinkedHashMap<>();
        routes.put("lodestar://example.com/app/test", TestActivity.class);
        return Collections.unmodifiableMap(routes);
    }

    public static class TestActivity extends Activity {}
}
