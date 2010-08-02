/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

package org.jf;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jf.smali.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles files in the smali assembly language
 *
 * @goal assemble
 *
 * @phase compile
 */
public class SmaliMojo
    extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter default-value="${basedir}/src/main/smali"
     * @required
     */
    private File sourceDirectory;

    /**
     * @parameter default-value="${project.build.directory}/classes.dex"
     * @required
     */
    private File outputFile;

    /**
     * @parameter default-value=null
     */
    private File dumpFile;

    public void execute()
        throws MojoExecutionException
    {
        outputFile.getParentFile().mkdirs();

        try
        {
            List<String> args = new ArrayList<String>();
            args.add("-o");
            args.add(outputFile.getAbsolutePath());


            if (dumpFile != null) {
                args.add("-D");
                args.add(dumpFile.getAbsolutePath());
            }

            args.add(sourceDirectory.getAbsolutePath());

            main.main(args.toArray(new String[args.size()]));
        } catch (Exception ex)
        {
            throw new MojoExecutionException("oops!", ex);
        }
    }
}
