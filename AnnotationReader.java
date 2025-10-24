package test;

import java.lang.reflect.*;

public class AnnotationReader {
    public static void main(String[] args) {
        Class<?> clazz = Controller.class;
        System.out.println("+--------------------------+");
        for (Method meth : clazz.getDeclaredMethods()) {
            if (meth.isAnnotationPresent(RouteMapping.class)) {
                RouteMapping route = meth.getAnnotation(RouteMapping.class);
                System.out.println("| Methode : " + meth.getName());
                System.out.println("| Path : " + route.url());
            }
        }
        System.out.println("+--------------------------+");
    }
}
