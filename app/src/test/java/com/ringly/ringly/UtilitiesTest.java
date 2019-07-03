package com.ringly.ringly;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by peter on 12/2/16.
 */

public class UtilitiesTest {
    @org.junit.Test
    public void testCompareVersions() {
        assertThat("empty vs number", Utilities.compareVersions("", "1"), is(-1));
        assertThat("number vs empty", Utilities.compareVersions("1", ""), is(1));

        assertThat("test vs non-test", Utilities.compareVersions("1.1.0-abc", "1.1.0"), is(0));
        assertThat("non-test vs test", Utilities.compareVersions("1.1.0", "1.1.0-abc"), is(0));

        assertThat("major+1 is greater", Utilities.compareVersions("2.2.0", "1.2.0"), is(1));
        assertThat("major-1 is less", Utilities.compareVersions("1.2.0", "2.2.0"), is(-1));

        assertThat("bug+1 is greater", Utilities.compareVersions("3.2.2", "3.2.1"), is(1));
        assertThat("bug-1 is less", Utilities.compareVersions("3.2.1", "3.2.2"), is(-1));

        assertThat("same + bug is greater", Utilities.compareVersions("1.0.1", "1.0"), is(1));
        assertThat("same + 2 bug is greater 2", Utilities.compareVersions("1.0.2", "1.0"), is(2));
    }
}
