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

import com.intellij.diagnostic.IdeErrorsDialog;
import com.intellij.diagnostic.LogMessageEx;
import com.intellij.diagnostic.ReportMessages;
import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.IdeaLogger;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;

import java.awt.*;
import java.util.Map;

/**
 * Sends crash reports to Github.
 *
 * Based on the go-lang plugin's error reporter
 * (https://github.com/dlsniper/google-go-lang-idea-plugin/commit/c451006cc9fc926ca347189951baa94f4032c5c4)
 */
public class ErrorReporter extends ErrorReportSubmitter {

  @Override
  public String getReportActionText() {
    return "Report as issue on smali's github repo";
  }

  @Override
  public boolean submit(IdeaLoggingEvent[] events, String additionalInfo, Component parentComponent,
                        final Consumer<SubmittedReportInfo> consumer) {
    IdeaLoggingEvent event = events[0];
    ErrorBean bean = new ErrorBean(event.getThrowable(), IdeaLogger.ourLastActionId);

    final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);

    bean.setDescription(additionalInfo);
    bean.setMessage(event.getMessage());

    Throwable throwable = event.getThrowable();
    if (throwable != null) {
      final PluginId pluginId = IdeErrorsDialog.findPluginId(throwable);
      if (pluginId != null) {
        final IdeaPluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(pluginId);
        if (ideaPluginDescriptor != null && !ideaPluginDescriptor.isBundled()) {
          bean.setPluginName(ideaPluginDescriptor.getName());
          bean.setPluginVersion(ideaPluginDescriptor.getVersion());
        }
      }
    }

    Object data = event.getData();

    if (data instanceof LogMessageEx) {
      bean.setAttachments(((LogMessageEx)data).getAttachments());
    }

    Map<String, String> reportValues = ITNProxy.createParameters(bean);

    final Project project = CommonDataKeys.PROJECT.getData(dataContext);

    Consumer<String> successCallback = new Consumer<String>() {
      @Override
      public void consume(String token) {
        final SubmittedReportInfo reportInfo = new SubmittedReportInfo(
                null, "Issue " + token, SubmittedReportInfo.SubmissionStatus.NEW_ISSUE);
        consumer.consume(reportInfo);

        ReportMessages.GROUP.createNotification(ReportMessages.ERROR_REPORT,
                "Submitted",
                NotificationType.INFORMATION,
                null).setImportant(false).notify(project);
      }
    };

    Consumer<Exception> errorCallback = new Consumer<Exception>() {
      @Override
      public void consume(Exception e) {
        String message = String.format("<html>There was an error while creating a GitHub issue: %s<br>" +
                "Please consider manually creating an issue on the " +
                "<a href=\"https://github.com/JesusFreke/smali/issues\">Smali Issue Tracker</a></html>",
                e.getMessage());
        ReportMessages.GROUP.createNotification(ReportMessages.ERROR_REPORT,
                message,
                NotificationType.ERROR,
                NotificationListener.URL_OPENING_LISTENER).setImportant(false).notify(project);
      }
    };

    GithubFeedbackTask task = new GithubFeedbackTask(project, "Submitting error report", true,  reportValues,
            successCallback, errorCallback);

    if (project == null) {
      task.run(new EmptyProgressIndicator());
    } else {
      ProgressManager.getInstance().run(task);
    }
    return true;
  }
}
