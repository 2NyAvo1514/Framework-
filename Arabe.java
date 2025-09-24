package pack;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Arabe {
    String cheminWeb();
    String method() default "GET";
}
