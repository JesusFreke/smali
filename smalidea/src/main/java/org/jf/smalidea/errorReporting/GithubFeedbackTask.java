/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jf.smalidea.errorReporting;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

public class GithubFeedbackTask extends Task.Backgroundable {
    private final Consumer<String> myCallback;
    private final Consumer<Exception> myErrorCallback;
    private final Map<String, String> myParams;

    public GithubFeedbackTask(@Nullable Project project,
                              @NotNull String title,
                              boolean canBeCancelled,
                              Map<String, String> params,
                              final Consumer<String> callback,
                              final Consumer<Exception> errorCallback) {
        super(project, title, canBeCancelled);

        myParams = params;
        myCallback = callback;
        myErrorCallback = errorCallback;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        try {
            String token = sendFeedback(myParams);
            myCallback.consume(token);
        }
        catch (Exception e) {
            myErrorCallback.consume(e);
        }
    }

    private static String getToken() {
        InputStream stream = GithubFeedbackTask.class.getClassLoader().getResourceAsStream("token");
        if (stream == null) {
            return null;
        }
        try {
            return CharStreams.toString(new InputStreamReader(stream, "UTF-8"));
        } catch (IOException ex) {
            return null;
        }
    }

    public static String sendFeedback(Map<String, String> environmentDetails) throws IOException {
        String url = "https://api.github.com/repos/JesusFreke/smalidea-issues/issues";
        String userAgent = "smalidea plugin";

        IdeaPluginDescriptorImpl pluginDescriptor =
                (IdeaPluginDescriptorImpl) PluginManager.getPlugin(PluginId.getId("org.jf.smalidea"));

        if (pluginDescriptor != null) {
            String name = pluginDescriptor.getName();
            String version = pluginDescriptor.getVersion();
            userAgent = name + " (" + version + ")";
        }

        HttpURLConnection httpURLConnection = connect(url);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("User-Agent", userAgent);
        httpURLConnection.setRequestProperty("Content-Type", "application/json");

        String token = getToken();
        if (token != null) {
            httpURLConnection.setRequestProperty("Authorization", "token " + token);
        }
        OutputStream outputStream = httpURLConnection.getOutputStream();

        try {
            outputStream.write(convertToGithubIssueFormat(environmentDetails));
        } finally {
            outputStream.close();
        }

        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode != 201) {
            throw new RuntimeException("Expected HTTP_CREATED (201), obtained " + responseCode);
        }

        return Long.toString(System.currentTimeMillis());
    }

    private static byte[] convertToGithubIssueFormat(Map<String, String> environmentDetails) {
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>(5);
        result.put("title", "[auto-generated] Crash in plugin");
        result.put("body", generateGithubIssueBody(environmentDetails));

        return ((new Gson()).toJson(result)).getBytes(Charset.forName("UTF-8"));
    }

    private static String generateGithubIssueBody(Map<String, String> body) {
        String errorDescription = body.get("error.description");
        if (errorDescription == null) {
            errorDescription = "";
        }
        body.remove("error.description");

        String errorMessage = body.get("error.message");
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "invalid error";
        }
        body.remove("error.message");

        String stackTrace = body.get("error.stacktrace");
        if (stackTrace == null || stackTrace.isEmpty()) {
            stackTrace = "invalid stacktrace";
        }
        body.remove("error.stacktrace");

        String result = "";

        if (!errorDescription.isEmpty()) {
            result += errorDescription + "\n\n";
        }

        for (Map.Entry<String, String> entry : body.entrySet()) {
            result += entry.getKey() + ": " + entry.getValue() + "\n";
        }

        result += "\n```\n" + stackTrace + "\n```\n";

        result += "\n```\n" + errorMessage + "\n```";

        return result;
    }

    private static HttpURLConnection connect(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) ((new URL(url)).openConnection());
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }
}
