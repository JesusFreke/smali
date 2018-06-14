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
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.jf.util.WrappedIndentingWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpFormatter {

    private int width = 80;

    @Nonnull
    public HelpFormatter width(int width) {
        this.width = width;
        return this;
    }

    @Nonnull
    private static ExtendedParameters getExtendedParameters(JCommander jc) {
        ExtendedParameters anno = jc.getObjects().get(0).getClass().getAnnotation(ExtendedParameters.class);
        if (anno == null) {
            throw new IllegalStateException("All commands should have an ExtendedParameters annotation");
        }
        return anno;
    }

    @Nonnull
    private static List<String> getCommandAliases(JCommander jc) {
        return Lists.newArrayList(getExtendedParameters(jc).commandAliases());
    }

    private static boolean includeParametersInUsage(@Nonnull JCommander jc) {
        return getExtendedParameters(jc).includeParametersInUsage();
    }

    @Nonnull
    private static String getPostfixDescription(@Nonnull JCommander jc) {
        return getExtendedParameters(jc).postfixDescription();
    }

    private int getParameterArity(ParameterDescription param) {
        if (param.getParameter().arity() > 0) {
            return param.getParameter().arity();
        }
        Class<?> type = param.getParameterized().getType();
        if ((type == boolean.class || type == Boolean.class)) {
            return 0;
        }
        return 1;
    }

    private List<ParameterDescription> getSortedParameters(JCommander jc) {
        List<ParameterDescription> parameters = Lists.newArrayList(jc.getParameters());

        final Pattern pattern = Pattern.compile("^-*(.*)$");

        Collections.sort(parameters, new Comparator<ParameterDescription>() {
            @Override public int compare(ParameterDescription o1, ParameterDescription o2) {
                String s1;
                Matcher matcher = pattern.matcher(o1.getParameter().names()[0]);
                if (matcher.matches()) {
                    s1 = matcher.group(1);
                } else {
                    throw new IllegalStateException();
                }

                String s2;
                matcher = pattern.matcher(o2.getParameter().names()[0]);
                if (matcher.matches()) {
                    s2 = matcher.group(1);
                } else {
                    throw new IllegalStateException();
                }

                return s1.compareTo(s2);
            }
        });
        return parameters;
    }

    @Nonnull
    public String format(@Nonnull JCommander... jc) {
        return format(Arrays.asList(jc));
    }

    @Nonnull
    public String format(@Nonnull List<JCommander> commandHierarchy) {
        try {
            StringWriter stringWriter = new StringWriter();
            WrappedIndentingWriter writer = new WrappedIndentingWriter(stringWriter, width - 5, width);

            JCommander leafJc = Iterables.getLast(commandHierarchy);

            writer.write("usage:");
            writer.indent(2);

            for (JCommander jc: commandHierarchy) {
                writer.write(" ");
                writer.write(ExtendedCommands.commandName(jc));
            }

            if (includeParametersInUsage(leafJc)) {
                for (ParameterDescription param : leafJc.getParameters()) {
                    if (!param.getParameter().hidden()) {
                        writer.write(" [");
                        writer.write(param.getParameter().getParameter().names()[0]);
                        writer.write("]");
                    }
                }
            } else {
                if (!leafJc.getParameters().isEmpty()) {
                    writer.write(" [<options>]");
                }
            }

            if (!leafJc.getCommands().isEmpty()) {
                writer.write(" [<command [<args>]]");
            }

            if (leafJc.getMainParameter() != null) {
                String[] argumentNames = ExtendedCommands.parameterArgumentNames(leafJc.getMainParameter());
                if (argumentNames.length == 0) {
                    writer.write(" <args>");
                } else {
                    String argumentName = argumentNames[0];
                    boolean writeAngleBrackets = !argumentName.startsWith("<") && !argumentName.startsWith("[");
                    writer.write(" ");
                    if (writeAngleBrackets) {
                        writer.write("<");
                    }
                    writer.write(argumentNames[0]);
                    if (writeAngleBrackets) {
                        writer.write(">");
                    }
                }
            }

            writer.deindent(2);

            String commandDescription = ExtendedCommands.getCommandDescription(leafJc);
            if (commandDescription != null) {
                writer.write("\n");
                writer.write(commandDescription);
            }

            if (!leafJc.getParameters().isEmpty() || leafJc.getMainParameter() != null) {
                writer.write("\n\nOptions:");
                writer.indent(2);
                for (ParameterDescription param : getSortedParameters(leafJc)) {
                    if (!param.getParameter().hidden()) {
                        writer.write("\n");
                        writer.indent(4);
                        if (!param.getNames().isEmpty()) {
                            writer.write(Joiner.on(',').join(param.getParameter().names()));
                        }
                        if (getParameterArity(param) > 0) {
                            String[] argumentNames = ExtendedCommands.parameterArgumentNames(param);
                            for (int i = 0; i < getParameterArity(param); i++) {
                                writer.write(" ");
                                if (i < argumentNames.length) {
                                    writer.write("<");
                                    writer.write(argumentNames[i]);
                                    writer.write(">");
                                } else {
                                    writer.write("<arg>");
                                }
                            }
                        }
                        if (param.getDescription() != null && !param.getDescription().isEmpty()) {
                            writer.write(" - ");
                            writer.write(param.getDescription());
                        }
                        if (param.getDefault() != null) {
                            String defaultValue = null;
                            if (param.getParameterized().getType() == Boolean.class ||
                                    param.getParameterized().getType() == Boolean.TYPE) {
                                if ((Boolean)param.getDefault()) {
                                    defaultValue = "True";
                                }
                            } else if (List.class.isAssignableFrom(param.getParameterized().getType())) {
                                if (!((List)param.getDefault()).isEmpty()) {
                                    defaultValue = param.getDefault().toString();
                                }
                            } else {
                                defaultValue = param.getDefault().toString();
                            }
                            if (defaultValue != null) {
                                writer.write(" (default: ");
                                writer.write(defaultValue);
                                writer.write(")");
                            }
                        }
                        writer.deindent(4);
                    }
                }

                if (leafJc.getMainParameter() != null) {
                    String[] argumentNames = ExtendedCommands.parameterArgumentNames(leafJc.getMainParameter());
                    writer.write("\n");
                    writer.indent(4);
                    if (argumentNames.length > 0) {
                        writer.write("<");
                        writer.write(argumentNames[0]);
                        writer.write(">");
                    } else {
                        writer.write("<args>");
                    }

                    if (leafJc.getMainParameterDescription() != null) {
                        writer.write(" - ");
                        writer.write(leafJc.getMainParameterDescription());
                    }
                    writer.deindent(4);
                }
                writer.deindent(2);
            }

            if (!leafJc.getCommands().isEmpty()) {
                writer.write("\n\nCommands:");
                writer.indent(2);


                List<Entry<String, JCommander>> entryList = Lists.newArrayList(leafJc.getCommands().entrySet());
                Collections.sort(entryList, new Comparator<Entry<String, JCommander>>() {
                    @Override public int compare(Entry<String, JCommander> o1, Entry<String, JCommander> o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });

                for (Entry<String, JCommander> entry : entryList) {
                    String commandName = entry.getKey();
                    JCommander command = entry.getValue();

                    Object arg = command.getObjects().get(0);
                    Parameters parametersAnno = arg.getClass().getAnnotation(Parameters.class);
                    if (!parametersAnno.hidden()) {
                        writer.write("\n");
                        writer.indent(4);
                        writer.write(commandName);
                        List<String> aliases = getCommandAliases(command);
                        if (!aliases.isEmpty()) {
                            writer.write("(");
                            writer.write(Joiner.on(',').join(aliases));
                            writer.write(")");
                        }

                        String commandDesc = leafJc.getCommandDescription(commandName);
                        if (commandDesc != null) {
                            writer.write(" - ");
                            writer.write(commandDesc);
                        }
                        writer.deindent(4);
                    }
                }
                writer.deindent(2);
            }

            String postfixDescription = getPostfixDescription(leafJc);
            if (!postfixDescription.isEmpty()) {
                writer.write("\n\n");
                writer.write(postfixDescription);
            }

            writer.flush();

            return stringWriter.getBuffer().toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
