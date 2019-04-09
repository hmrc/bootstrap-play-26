package uk.gov.hmrc.play.bootstrap.binding;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@BindingAnnotation
@Target({FIELD, PARAMETER, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AppName {}
