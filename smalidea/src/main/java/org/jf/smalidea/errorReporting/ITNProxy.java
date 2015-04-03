/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jf.smalidea.errorReporting;

import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;

import java.util.Calendar;
import java.util.Map;

/**
 * @author stathik
 * @since Aug 4, 2003
 */
public class ITNProxy {

  public static Map<String, String> createParameters(ErrorBean error) {
    Map<String, String> params = ContainerUtil.newLinkedHashMap(40);

    params.put("protocol.version", "1");

    params.put("os.name", SystemProperties.getOsName());
    params.put("java.version", SystemProperties.getJavaVersion());
    params.put("java.vm.vendor", SystemProperties.getJavaVmVendor());

    ApplicationInfoEx appInfo = ApplicationInfoEx.getInstanceEx();
    ApplicationNamesInfo namesInfo = ApplicationNamesInfo.getInstance();
    Application application = ApplicationManager.getApplication();
    params.put("app.name", namesInfo.getProductName());
    params.put("app.name.full", namesInfo.getFullProductName());
    params.put("app.name.version", appInfo.getVersionName());
    params.put("app.eap", Boolean.toString(appInfo.isEAP()));
    params.put("app.internal", Boolean.toString(application.isInternal()));
    params.put("app.build", appInfo.getBuild().asString());
    params.put("app.version.major", appInfo.getMajorVersion());
    params.put("app.version.minor", appInfo.getMinorVersion());
    params.put("app.build.date", format(appInfo.getBuildDate()));
    params.put("app.build.date.release", format(appInfo.getMajorReleaseBuildDate()));
    params.put("app.compilation.timestamp", IdeaLogger.getOurCompilationTimestamp());

    UpdateSettings updateSettings = UpdateSettings.getInstance();
    params.put("update.channel.status", updateSettings.getSelectedChannelStatus().getCode());
    params.put("update.ignored.builds", StringUtil.join(updateSettings.getIgnoredBuildNumbers(), ","));

    params.put("plugin.name", error.getPluginName());
    params.put("plugin.version", error.getPluginVersion());

    params.put("last.action", error.getLastAction());
    params.put("previous.exception", error.getPreviousException() == null ? null : Integer.toString(error.getPreviousException()));

    params.put("error.message", error.getMessage());
    params.put("error.stacktrace", error.getStackTrace());
    params.put("error.description", error.getDescription());

    params.put("assignee.id", error.getAssigneeId() == null ? null : Integer.toString(error.getAssigneeId()));

    for (Attachment attachment : error.getAttachments()) {
      params.put("attachment.name", attachment.getName());
      params.put("attachment.value", attachment.getEncodedBytes());
    }

    return params;
  }

  private static String format(Calendar calendar) {
    return calendar == null ?  null : Long.toString(calendar.getTime().getTime());
  }
}
