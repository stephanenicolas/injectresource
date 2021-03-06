package com.github.stephanenicolas.injectresource;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import com.test.injectresource.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author SNI
 */
@RunWith(InjectResourceTestRunner.class)
public class InjectResourceProcessorInActivityTest {
  public static final int RESOURCE_ID_STRING = R.string.string1;
  public static final int RESOURCE_ID_INTEGER = R.integer.integer1;
  public static final int RESOURCE_ID_BOOLEAN = R.bool.bool1;
  public static final int RESOURCE_ID_STRING_ARRAY = R.array.array1;
  public static final int RESOURCE_ID_INTEGER_ARRAY = R.array.intarray1;
  public static final int RESOURCE_ID_MOVIE = R.raw.small;
  public static final int RESOURCE_ID_ANIMATION = android.R.anim.fade_in;
  public static final int RESOURCE_ID_COLOR_STATE_LIST = R.color.colorlist;
  public static final int RESOURCE_ID_DRAWABLE = R.drawable.ic_launcher;
  public static final int RESOURCE_BAD_ID = 1111111;

  @Test
  public void shouldInjectResource_simple() {
    TestActivity activity = Robolectric.buildActivity(TestActivity.class).create().get();
    assertThat(activity.string,
        is(Robolectric.application.getResources().getString(RESOURCE_ID_STRING)));
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
    //doesn't work on Robolectric..
    //assertNotNull(activity.movie);
    assertNotNull(activity.colorStateList);
    assertNotNull(activity.drawable);
  }

  @Test(expected = RuntimeException.class)
  public void shouldInjectResource_badResourceType() {
    TestActivityWithBadResourceType activity =
        Robolectric.buildActivity(TestActivityWithBadResourceType.class).create().get();
  }

  @Test(expected = RuntimeException.class)
  public void shouldInjectResource_badResourceTypeArray() {
    TestActivityWithBadResourceTypeArray activity =
        Robolectric.buildActivity(TestActivityWithBadResourceTypeArray.class).create().get();
  }

  @Test(expected = RuntimeException.class)
  public void shouldInjectResource_badResourceId() {
    TestActivityWithBadResourceID activity =
        Robolectric.buildActivity(TestActivityWithBadResourceID.class).create().get();
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
    @InjectResource(RESOURCE_ID_MOVIE)
    protected Movie movie;
    @InjectResource(RESOURCE_ID_ANIMATION)
    protected Animation anim;
    @InjectResource(RESOURCE_ID_COLOR_STATE_LIST)
    protected ColorStateList colorStateList;
    @InjectResource(RESOURCE_ID_DRAWABLE)
    protected Drawable drawable;

    //short name case.
    @InjectResource(RESOURCE_ID_STRING)
    protected String s;
  }

  public static class TestActivityWithBadResourceType extends Activity {
    @InjectResource(RESOURCE_ID_STRING)
    protected Foo badResource;
  }

  public static class TestActivityWithBadResourceTypeArray extends Activity {
    @InjectResource(RESOURCE_ID_STRING)
    protected boolean[] badResource;
  }

  public static class TestActivityWithBadResourceID extends Activity {
    @InjectResource(RESOURCE_BAD_ID)
    protected String badResource;
  }

  private static class Foo {
  }
}
