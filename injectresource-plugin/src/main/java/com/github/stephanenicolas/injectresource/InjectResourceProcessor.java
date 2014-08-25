package com.github.stephanenicolas.injectresource;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.ColorStateList;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import com.github.stephanenicolas.afterburner.AfterBurner;

import com.github.stephanenicolas.afterburner.exception.AfterBurnerImpossibleException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.build.IClassTransformer;
import javassist.build.JavassistBuildException;
import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;

/**
 * Will inject all fields and fragments from XML.
 *
 * <pre>
 * <ul>
 *   <li>for activities :
 *     <ul>
 *       <li>if they use @ContentView : right after super.onCreate
 *       <li>if they don't use @ContentView : right after setContentView invocation in onCreate
 *       <li>it doesn't matter if you supply your own version of onCreate or setContenView or not.
 *     </ul>
 *   <li>for fragments :
 *     <ul>
 *       <li>right after onViewCreated
 *       <li>views are destroyed right after onViewDestroyed
 *     </ul>
 *   <li>for views :
 *     <ul>
 *       <li>right after onFinishInflate
 *       <li>onFinishInflate is called automatically by Android when inflating a view from XML
 *       <li>onFinishInflate must be called manually in constructors of views with a single context
 * argument. You should invoke it after inflating your layout manually.
 *     </ul>
 *   <li>for other classes (namely MVP presenters and view holder design patterns) :
 *     <ul>
 *       <li>right before any constructor with a single argument of type Activity, Fragment, or
 * View
 *       <li>static inner classes can only be processed if static
 *     </ul>
 * </ul>
 * </pre>
 *
 * @author SNI
 */
@Slf4j
public class InjectResourceProcessor implements IClassTransformer {

  AfterBurner afterBurner = new AfterBurner();

  @Override
  public boolean shouldTransform(CtClass candidateClass) throws JavassistBuildException {
    try {
      final List<CtField> resources =
          getAllInjectedFieldsForAnnotation(candidateClass, InjectResource.class);
      ClassPool classPool = candidateClass.getClassPool();
      boolean isActivity = candidateClass.subtypeOf(classPool.get(Activity.class.getName()));
      boolean shouldTransform = !resources.isEmpty();
      log.debug(
          "Class " + candidateClass.getSimpleName() + " will get transformed: " + shouldTransform);
      return shouldTransform;
    } catch (NotFoundException e) {
      log.debug(format("Error while filtering class %s", candidateClass.getName()), e);
      return false;
    }
  }

  @Override
  public void applyTransformations(final CtClass classToTransform) throws JavassistBuildException {
    // Actually you must test if it exists, but it's just an example...
    log.debug("Analyzing " + classToTransform.getSimpleName());

    try {
      final List<CtField> fields =
          getAllInjectedFieldsForAnnotation(classToTransform, InjectResource.class);
      String getApplicationString = getApplicationString(classToTransform);
      String getResourceString = getResourceString(getApplicationString);
      String injectViewStatements = injectResourceStatements(fields, classToTransform, getApplicationString);
      StringBuffer buffer = new StringBuffer();
      buffer.append(getResourceString);
      buffer.append(";\n");
      buffer.append(injectViewStatements);
      String body = buffer.toString();
      log.debug("Inserted :" + body);
      injectStuff(classToTransform, body);
    } catch (Exception e) {
      log.debug(format("Error while processing class %s", classToTransform.getName()), e);
    }
  }

  private void injectStuff(CtClass classToTransform, String body)
      throws CannotCompileException, AfterBurnerImpossibleException, NotFoundException {
    //TODO only works for activities
    afterBurner.afterOverrideMethod(classToTransform, "onCreate", body);
  }


  private String getApplicationString(CtClass targetClazz) {
    boolean isActivity = isActivity(targetClazz);
    boolean isFragment = isFragment(targetClazz);
    boolean isSupportFragment = isSupportFragment(targetClazz);
    boolean isView = isView(targetClazz);

    //TODO : only works for activities
    return "getApplication()";
  }

  private String getResourceString(String getApplicationString) {
    return "android.content.res.Resources resources = " + getApplicationString + ".getResources()";
  }

  private List<CtField> getAllInjectedFieldsForAnnotation(CtClass clazz,
      Class<? extends Annotation> annotationClazz) {
    List<CtField> result = new ArrayList<CtField>();
    CtField[] allFields = clazz.getDeclaredFields();
    log.debug("Scanning fields in " + clazz.getName());
    for (CtField field : allFields) {
      log.debug("Discovered field " + field.getName());
      if (field.hasAnnotation(annotationClazz)) {
        log.debug(
            "Field " + field.getName() + " has annotation " + annotationClazz.getSimpleName());
        result.add(field);
      }
    }
    return result;
  }

  private String injectResourceStatements(List<CtField> viewsToInject, CtClass targetClazz, String getApplicationString)
      throws ClassNotFoundException, NotFoundException {
    StringBuffer buffer = new StringBuffer();
    for (CtField field : viewsToInject) {
      Object annotation = field.getAnnotation(InjectResource.class);
      //must be accessed by introspection as I get a Proxy during tests.
      //this proxy comes from Robolectric
      Class annotionClass = annotation.getClass();

      //workaround for robolectric
      //https://github.com/robolectric/robolectric/pull/1240
      int id = 0;
      String tag = "";
      try {
        Method method = annotionClass.getMethod("value");
        id = (Integer) method.invoke(annotation);
        method = annotionClass.getMethod("name");
        tag = (String) method.invoke(annotation);
      } catch (Exception e) {
        throw new RuntimeException("How can we get here ?");
      }
      boolean isUsingId = id != -1;

      buffer.append(field.getName());
      buffer.append(" = ");

      String root = "resources";
      String findResourceString = "";
      ClassPool classPool = targetClazz.getClassPool();
      if (isSubClass(classPool, field.getType(), String.class)) {
        findResourceString = "getString(" + id + ")";
      } else if (field.getType().subtypeOf(CtClass.booleanType)) {
        findResourceString = "getBoolean(" + id + ")";
      } else if (isSubClass(classPool, field.getType(), Boolean.class)) {
        root = null;
        findResourceString = "new Boolean(resources.getBoolean(" + id + "))";
      } else if (isSubClass(classPool, field.getType(), ColorStateList.class)) {
        findResourceString = "getColorStateList(" + id + ")";
      } else if (field.getType().subtypeOf(CtClass.intType)) {
        findResourceString = "getInteger(" + id + ")";
      } else if (isSubClass(classPool, field.getType(), Integer.class)) {
        root = null;
        findResourceString = "new Integer(resources.getInteger(" + id + "))";
      } else if (isSubClass(classPool, field.getType(), Drawable.class)) {
        findResourceString = "getDrawable(" + id + ")";
      } else if (field.getType().isArray() && isSubClass(classPool, field.getType().getComponentType(), String.class)) {
        findResourceString = "getStringArray(" + id + ")";
      } else if (field.getType().isArray() && field.getType().getComponentType().subtypeOf(
          CtClass.intType)) {
        findResourceString = "getIntArray(" + id + ")";
      } else if (isSubClass(classPool, field.getType(), Animation.class)) {
        root = null;
        findResourceString = "android.view.animation.AnimationUtils.loadAnimation(" +getApplicationString + ", " + id + ")";
      } else if (isSubClass(classPool, field.getType(), Movie.class)) {
        findResourceString = "getMovie(" + id + ")";
      } else {
        throw new NotFoundException(
            format("Don't know how to inject field %s of type %s in %s", field.getName(),
                field.getType().getName(), targetClazz.getName()));
      }
      if (root != null) {
        buffer.append(root);
        buffer.append(".");
      }
      buffer.append(findResourceString);
      buffer.append(";\n");
    }
    return buffer.toString();
  }

  private boolean isActivity(CtClass clazz) {
    try {
      return isSubClass(clazz.getClassPool(), clazz, Activity.class);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isFragment(CtClass clazz) {
    try {
      return isSubClass(clazz.getClassPool(), clazz, Fragment.class);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isSupportFragment(CtClass clazz) {
    try {
      Class<?> supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
      return isSubClass(clazz.getClassPool(), clazz, supportFragmentClass);
    } catch (Exception e) {
      //can happen
      return false;
    }
  }

  private boolean isView(CtClass clazz) {
    try {
      return isSubClass(clazz.getClassPool(), clazz, View.class);
    } catch (NotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isSubClass(ClassPool classPool, CtClass clazz, Class<?> superClass) throws NotFoundException {
    CtClass superclass = classPool.get(superClass.getName());
    return clazz.subclassOf(superclass);
  }
}
