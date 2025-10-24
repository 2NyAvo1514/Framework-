package test;

public class Controller {
    @RouteMapping(url = "/hello")
    public void sayHello(){
        System.out.println("Hello world !");
    }
    @RouteMapping(url = "/bye")
    public void sayBye(){
        System.out.println("Bye world !");
    }
}
