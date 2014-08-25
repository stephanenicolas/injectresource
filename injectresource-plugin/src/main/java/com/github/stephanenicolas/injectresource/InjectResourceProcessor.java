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
import java.util.regex.Matcher;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.build.IClassTransformer;
import javassist.build.JavassistBuildException;
import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;

/**
 * Will inject all resources.
 *
 * <pre>
 * <ul>
 *   <li>for activities :
 *     <ul>
 *       <li>right after super.onCreate in onCreate
 *     </ul>
 *   <li>for fragments :
 *     <ul>
 *       <li>right after super.onAttach in onAttach
 *     </ul>
 *   <li>for views :
 *     <ul>
 *       <li>right after super.onFinishInflate in onFinishInflate
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

  private final static String GET_APPLICATION_TAG = "GET_APPLICATION_TAG";
  private static final String GET_ROOT = "GET_ROOT";

  AfterBurner afterBurner = new AfterBurner();

  @Override
  public boolean shouldTransform(CtClass candidateClass) throws JavassistBuildException {
    try {
      final List<CtField> fields =
          getAllInjectedFieldsForAnnotation(candidateClass, InjectResource.class);

      if (fields.isEmpty()) {
        return false;
      }

      boolean isAcceptedClass = isActivity(candidateClass) || isFragment(candidateClass)
          || isSupportFragment(candidateClass) || isView(candidateClass);

      if (!isAcceptedClass) {
        List<CtConstructor> ctConstructors = extractValidConstructors(candidateClass);
        isAcceptedClass = !ctConstructors.isEmpty();
      }
      
      log.debug(
          "Class " + candidateClass.getSimpleName() + " will get transformed: " + isAcceptedClass);
      return isAcceptedClass;
    } catch (Exception e) {
      String message = format("Error while filtering class %s", candidateClass.getName());
      log.debug(message, e);
      throw new JavassistBuildException(message, e);
    }
  }

  @Override
  public void applyTransformations(final CtClass classToTransform) throws JavassistBuildException {
    // Actually you must test if it exists, but it's just an example...
    log.debug("Analyzing " + classToTransform.getSimpleName());

    try {
      final List<CtField> fields =
          getAllInjectedFieldsForAnnotation(classToTransform, InjectResource.class);
      String getResourceString = getResourceString();
      String injectViewStatements = injectResourceStatements(fields, classToTransform);
      StringBuffer buffer = new StringBuffer();
      buffer.append(getResourceString);
      buffer.append(";\n");
      buffer.append(injectViewStatements);
      String body = buffer.toString();
      log.debug("Inserted :" + body);
      injectStuff(classToTransform, body);
    } catch (Exception e) {
      String message = format("Error while processing class %s", classToTransform.getName());
      log.debug(message, e);
      throw new JavassistBuildException(message, e);
    }
  }

  private void injectStuff(CtClass targetClazz, String body)
      throws CannotCompileException, AfterBurnerImpossibleException, NotFoundException,
      InjectResourceException {
    boolean isActivity = isActivity(targetClazz);
    boolean isFragment = isFragment(targetClazz);
    boolean isSupportFragment = isSupportFragment(targetClazz);
    boolean isView = isView(targetClazz);

    if (isActivity) {
      body = insertApplicationString(targetClazz, body, "this");
      afterBurner.afterOverrideMethod(targetClazz, "onCreate", body);
    } else if (isFragment || isSupportFragment) {
      body = insertApplicationString(targetClazz, body, "this");
      afterBurner.afterOverrideMethod(targetClazz, "onAttach", body);
    } else if (isView) {
      body = insertApplicationString(targetClazz, body, "this");
      afterBurner.afterOverrideMethod(targetClazz, "onFinishInflate", body);
    } else {
      List<CtConstructor> ctConstructors = extractValidConstructors(targetClazz);
      if (ctConstructors.isEmpty()) {
        throw new InjectResourceException(format(
            "Injecting resource in %s is not supported. Injection is supported in Activities, "
                + "Fragments (native and support), views and classes with a single constructor "
                + "that accept a single paramater which has to "
                + "be of one of the type listed earlier.", targetClazz));
      }
      for (CtConstructor ctConstructor : ctConstructors) {
        CtClass ctParamClass = ctConstructor.getParameterTypes()[0];
        String bodyCopy = insertApplicationString(ctParamClass, body, "$1");
        log.debug(format("Body to be injected in constructor with type %s: %s", ctParamClass.getName(),
            bodyCopy));
        ctConstructor.insertBeforeBody(bodyCopy);
      }
    }
  }

  private String insertApplicationString(CtClass targetClazz, String body, String root) {
    //need a fresh copy for each call (constructor loop for instance)
    String bodyCopy = new String(body);
    String getApplicationString = getApplicationString(targetClazz);
    getApplicationString = getApplicationString.replaceAll(GET_ROOT, Matcher.quoteReplacement(root));
    bodyCopy = bodyCopy.replaceAll(GET_APPLICATION_TAG, Matcher.quoteReplacement(getApplicationString));
    return bodyCopy;
  }

  private String getApplicationString(CtClass targetClazz) {
    boolean isActivity = isActivity(targetClazz);
    boolean isFragment = isFragment(targetClazz);
    boolean isSupportFragment = isSupportFragment(targetClazz);
    boolean isView = isView(targetClazz);

    if (isActivity) {
      return GET_ROOT + ".getApplication()";
    } else if (isFragment || isSupportFragment) {
      return GET_ROOT + ".getActivity().getApplication()";
    } else if (isView) {
      return "((android.app.Application) " + GET_ROOT + ".getContext().getApplicationContext())";
    } else {
      throw new RuntimeException("How did we get there ?");
    }
  }

  private String getResourceString() {
    return "android.content.res.Resources resources = " + GET_APPLICATION_TAG + ".getResources()";
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

  private List<CtConstructor> extractValidConstructors(final CtClass classToTransform) {
    try {
      List<CtConstructor> constructors = new ArrayList<CtConstructor>();
      CtConstructor[] declaredConstructors = classToTransform.getDeclaredConstructors();
      for (CtConstructor constructor : declaredConstructors) {
        CtClass[] paramClasses = constructor.getParameterTypes();
        if (paramClasses.length >= 1) {
          if (isView(paramClasses[0])) {
            constructors.add(constructor);
          } else if (isActivity(paramClasses[0])) {
            constructors.add(constructor);
          } else if (isFragment(paramClasses[0])) {
            constructors.add(constructor);
          }  else if (isSupportFragment(paramClasses[0])) {
            constructors.add(constructor);
          }
        }
      }
      return constructors;
    } catch (Exception e) {
      return null;
    }
  }

  private String injectResourceStatements(List<CtField> viewsToInject, CtClass targetClazz) throws ClassNotFoundException, NotFoundException {
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
      } else if (isStringArray(field, classPool)) {
        findResourceString = "getStringArray(" + id + ")";
      } else if (isIntArray(field)) {
        findResourceString = "getIntArray(" + id + ")";
      } else if (isSubClass(classPool, field.getType(), Animation.class)) {
        root = null;
        findResourceString = "android.view.animation.AnimationUtils.loadAnimation("
            + GET_APPLICATION_TAG
            + ", "
            + id
            + ")";
      } else if (isSubClass(classPool, field.getType(), Movie.class)) {
        findResourceString = "getMovie(" + id + ")";
      } else {
        throw new NotFoundException(
            format("InjectResource doen't know how to inject field %s of type %s in %s",
                field.getName(), field.getType().getName(), targetClazz.getName()));
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

  private boolean isStringArray(CtField field, ClassPool classPool) throws NotFoundException {
    return field.getType().isArray() && isSubClass(classPool, field.getType().getComponentType(),
        String.class);
  }

  private boolean isIntArray(CtField field) throws NotFoundException {
    return field.getType().isArray() && field.getType()
        .getComponentType()
        .subtypeOf(CtClass.intType);
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

  private boolean isSubClass(ClassPool classPool, CtClass clazz, Class<?> superClass)
      throws NotFoundException {
    CtClass superclass = classPool.get(superClass.getName());
    return clazz.subclassOf(superclass);
  }
}
