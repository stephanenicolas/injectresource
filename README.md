InjectResource [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.stephanenicolas.injectresource/injectresource-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.stephanenicolas.injectresource/injectresource-plugin)
==========

***Injects resources automatically.***

<img src="https://raw.github.com/stephanenicolas/injectresource/master/assets/injectresource-logo.jpg"
width="150px" />

###Usage

Inside your `build.gradle` file, add : 

```groovy
apply plugin: 'injectresource'
```

And now, annotate every resource you want to inject.

```java

public class MainActivity extends Activity {
   @InjectResource(R.string.foo) private String fooResource;
}
```

###Example

You will find an example program in the repo.

###How does it work ?

Thanks to 
* [morpheus](https://github.com/stephanenicolas/morpheus), byte code weaver for android.
* [AfterBurner](https://github.com/stephanenicolas/afterburner), byte code weaving swiss army knife for Android.

### Related projects 

On the same principle of byte code weaving : 

* [InjectView](https://github.com/stephanenicolas/injectview)
* [InjectExtra](https://github.com/stephanenicolas/injectextra)
* [LogLifeCycle](https://github.com/stephanenicolas/loglifecycle)
* [Hugo](https://github.com/jakewharton/hugo)

### CI 

[![Travis Build](https://travis-ci.org/stephanenicolas/injectresource.svg?branch=master)](https://travis-ci.org/stephanenicolas/injectresource)
[![Coverage Status](https://img.shields.io/coveralls/stephanenicolas/injectresource.svg)](https://coveralls.io/r/stephanenicolas/injectresource)
