package mg.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME) // indispensable pour que ce soit visible à l'exécution
public @interface Controller {

}

