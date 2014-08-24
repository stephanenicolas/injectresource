package com.github.stephanenicolas.injectresource;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javassist.CtClass;
import javassist.CtField;
import javassist.build.IClassTransformer;
import javassist.build.JavassistBuildException;
import lombok.extern.slf4j.Slf4j;

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
 *       <li>right before any constructor with a single argument of type Activity, Fragment, or View
 *       <li>static inner classes can only be processed if static
 *     </ul>
 * </ul>
 * </pre>
 * @author SNI
 */
@Slf4j
public class InjectResourceProcessor implements IClassTransformer {

  @Override
  public boolean shouldTransform(CtClass candidateClass) throws JavassistBuildException {
    boolean hasAfterBurner = checkIfAfterBurnerAlreadyActive(candidateClass);
    final List<CtField> resources = getAllInjectedFieldsForAnnotation(candidateClass, com.github.stephanenicolas.injectresource.InjectResource.class);
    boolean shouldTransform = !hasAfterBurner && !resources.isEmpty();
    log.debug("Class " + candidateClass.getSimpleName() + " will get transformed: " + shouldTransform);
    return shouldTransform;
  }

  @Override
  public void applyTransformations(final CtClass classToTransform) throws JavassistBuildException {
    // Actually you must test if it exists, but it's just an example...
    log.debug("Analyzing " + classToTransform.getSimpleName());
  }



  private boolean checkIfAfterBurnerAlreadyActive(final CtClass classToTransform) {
    try {
      classToTransform.getDeclaredField("afterBurnerActive");
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private List<CtField> getAllInjectedFieldsForAnnotation(CtClass clazz, Class<? extends Annotation> annotationClazz) {
    List<CtField> result = new ArrayList<CtField>();
    CtField[] allFields = clazz.getDeclaredFields();
    log.debug("Scanning fields in " + clazz.getName());
    for (CtField field : allFields) {
      log.debug("Discovered field " + field.getName());
      if (field.hasAnnotation(annotationClazz)) {
        log.debug("Field " + field.getName() + " has annotation " + annotationClazz.getSimpleName());
        result.add(field);
      }
    }
    return result;
  }

}
