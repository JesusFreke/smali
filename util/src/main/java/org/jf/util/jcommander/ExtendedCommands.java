/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util.jcommander;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameterized;
import com.beust.jcommander.Parameters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Utilities related to "extended" commands - JCommander commands with additional information
 */
public class ExtendedCommands {

    @Nonnull
    private static ExtendedParameters getExtendedParameters(Object command) {
        ExtendedParameters anno = command.getClass().getAnnotation(ExtendedParameters.class);
        if (anno == null) {
            throw new IllegalStateException("All extended commands should have an ExtendedParameters annotation: " +
                    command.getClass().getCanonicalName());
        }
        return anno;
    }

    @Nonnull
    public static String commandName(JCommander jc) {
        return getExtendedParameters(jc.getObjects().get(0)).commandName();
    }

    @Nonnull
    public static String commandName(Object command) {
        return getExtendedParameters(command).commandName();
    }

    @Nonnull
    public static String[] commandAliases(JCommander jc) {
        return commandAliases(jc.getObjects().get(0));
    }

    @Nonnull
    public static String[] commandAliases(Object command) {
        return getExtendedParameters(command).commandAliases();
    }

    public static boolean includeParametersInUsage(JCommander jc) {
        return includeParametersInUsage(jc.getObjects().get(0));
    }

    public static boolean includeParametersInUsage(Object command) {
        return getExtendedParameters(command).includeParametersInUsage();
    }

    @Nonnull
    public static String postfixDescription(JCommander jc) {
        return postfixDescription(jc.getObjects().get(0));
    }

    @Nonnull
    public static String postfixDescription(Object command) {
        return getExtendedParameters(command).postfixDescription();
    }

    public static void addExtendedCommand(JCommander jc, Command command) {
        jc.addCommand(commandName(command), command, commandAliases(command));
        command.setupCommand(command.getJCommander());
    }

    @Nonnull
    public static String[] parameterArgumentNames(ParameterDescription parameterDescription) {
        Parameterized parameterized = parameterDescription.getParameterized();

        Class cls = parameterDescription.getObject().getClass();
        Field field = null;
        while (cls != Object.class) {
            try {
                field = cls.getDeclaredField(parameterized.getName());
            } catch (NoSuchFieldException ex) {
                cls = cls.getSuperclass();
                continue;
            }
            break;
        }

        assert field != null;
        ExtendedParameter extendedParameter = field.getAnnotation(ExtendedParameter.class);
        if (extendedParameter != null) {
            return extendedParameter.argumentNames();
        }

        return new String[0];
    }

    @Nullable
    public static JCommander getSubcommand(JCommander jc, String commandName) {
        if (jc.getCommands().containsKey(commandName)) {
            return jc.getCommands().get(commandName);
        } else {
            for (JCommander command : jc.getCommands().values()) {
                for (String alias: commandAliases(command)) {
                    if (commandName.equals(alias)) {
                        return command;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static String getCommandDescription(@Nonnull JCommander jc) {
        Parameters parameters = jc.getObjects().get(0).getClass().getAnnotation(Parameters.class);
        if (parameters == null) {
            return null;
        }
        return parameters.commandDescription();
    }
}
