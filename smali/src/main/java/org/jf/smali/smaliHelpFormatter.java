package org.jf.smali;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class smaliHelpFormatter extends HelpFormatter {

    public void smaliHelpFormatter() {
    }

    public void renderOptions(StringBuffer sb, Options options) {
        super.renderOptions(sb, getWidth(), options, getLeftPadding(), this.getDescPadding());
    }
}
