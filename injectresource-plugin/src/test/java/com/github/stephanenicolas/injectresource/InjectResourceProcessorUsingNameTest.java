package com.github.stephanenicolas.injectresource;

import android.app.Activity;
import com.test.injectresource.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author SNI
 */
@RunWith(InjectResourceTestRunner.class)
public class InjectResourceProcessorUsingNameTest {
  public static final int RESOURCE_ID_STRING = R.string.string1;

  @Test
  public void shouldInjectResource_withName() {
    TestActivityWithResourceName activity =
        Robolectric.buildActivity(TestActivityWithResourceName.class).create().get();
    assertThat(activity.string,
        is(Robolectric.application.getResources().getString(RESOURCE_ID_STRING)));
  }

  public static class TestActivityWithResourceName extends Activity {
    @InjectResource(name = "com.test.injectresource:string/string1")
    protected String string;
  }
}
