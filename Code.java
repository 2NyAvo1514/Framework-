package pack ;

public class Code {
    public static String codeString(){
        return "Votre URL est passee par ici (Framework fait maison) !";
    }
    @Arabe(cheminWeb = "/andrana",method = "GET")
    public static String testAnnotationRoute() {
        return "Nety ilay andrana .";
    }
}
