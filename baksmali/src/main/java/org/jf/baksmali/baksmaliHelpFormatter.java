package org.jf.baksmali;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class baksmaliHelpFormatter extends HelpFormatter {

    public void baksmaliHelpFormatter() {
    }

    public void renderOptions(StringBuffer sb, Options options) {
        super.renderOptions(sb, getWidth(), options, getLeftPadding(), this.getDescPadding());
    }
}
