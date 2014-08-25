package com.github.stephanenicolas.injectresource;

import android.app.Activity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.test.injectresource.R;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.stephanenicolas.injectresource.InjectResourceTestRunner.*;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/**
 * These tests are really complex to setup.
 * Take your time for maintenance.
 * @author SNI
 */
@RunWith(InjectResourceTestRunner.class)
public class InjectResourceProcessorInActivityTest {
  public static final int RESOURCE_ID_STRING = R.string.string1;
  public static final int RESOURCE_ID_INTEGER = R.integer.integer1;
  public static final int RESOURCE_ID_BOOLEAN = R.bool.bool1;
  public static final int RESOURCE_ID_STRING_ARRAY = R.array.array1;
  public static final int RESOURCE_ID_INTEGER_ARRAY = R.array.intarray1;
  //public static final int RESOURCE_ID_MOVIE = android.R.movie.copy;
  public static final int RESOURCE_ID_ANIMATION = android.R.anim.fade_in;
  //public static final int RESOURCE_COLOR_STATE_LIST = android.R.color.

  private InjectResourceProcessor processor = new InjectResourceProcessor();

  @Test
  public void shouldInjectResource_simple() {
    TestActivity activity = Robolectric.buildActivity(TestActivity.class)
        .create()
        .get();
    assertThat(activity.string, is(Robolectric.application.getResources().getString(
        RESOURCE_ID_STRING)));
    assertThat(activity.intA,
        is(Robolectric.application.getResources().getInteger(RESOURCE_ID_INTEGER)));
    assertThat(activity.intB,
        is(Robolectric.application.getResources().getInteger(RESOURCE_ID_INTEGER)));
    assertThat(activity.boolA,
        is(Robolectric.application.getResources().getBoolean(RESOURCE_ID_BOOLEAN)));
    assertThat(activity.boolB,
        is(Robolectric.application.getResources().getBoolean(RESOURCE_ID_BOOLEAN)));
    assertThat(activity.arrayA,
        is(Robolectric.application.getResources().getStringArray(RESOURCE_ID_STRING_ARRAY)));
    assertThat(activity.arrayB,
        is(Robolectric.application.getResources().getIntArray(RESOURCE_ID_INTEGER_ARRAY)));
    assertNotNull(activity.anim);
  }

  public static class TestActivity extends Activity {
    @InjectResource(RESOURCE_ID_STRING)
    protected String string;
    @InjectResource(RESOURCE_ID_INTEGER)
    protected int intA;
    @InjectResource(RESOURCE_ID_INTEGER)
    protected Integer intB;
    @InjectResource(RESOURCE_ID_BOOLEAN)
    protected boolean boolA;
    @InjectResource(RESOURCE_ID_BOOLEAN)
    protected Boolean boolB;
    @InjectResource(RESOURCE_ID_STRING_ARRAY)
    protected String[] arrayA;
    @InjectResource(RESOURCE_ID_INTEGER_ARRAY)
    protected int[] arrayB;
    @InjectResource(RESOURCE_ID_ANIMATION)
    protected Animation anim;
  }
}