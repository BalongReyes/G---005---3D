package Core;

public class Console{


    public static final String RESET = "\u001b[0m";
    public static final String BLACK = "\u001b[0;30m";
    public static final String RED = "\u001b[0;31m";
    public static final String GREEN = "\u001b[0;32m";
    public static final String YELLOW = "\u001b[0;33m";
    public static final String BLUE = "\u001b[0;34m";
    public static final String PURPLE = "\u001b[0;35m";
    public static final String CYAN = "\u001b[0;36m";
    public static final String WHITE = "\u001b[0;37m";

// Main Methods ==============================================================================================
    
    public static void gap(){
        System.out.println();
    }

    public static void line(){
        out("----------------------------------------", "\u001b[0;32m");
        out("> ", "\u001b[0;32m", false);
    }

// -----------------------------------------------------------------------------------------------------------
    
    public static <E> void out(E[] arrayOutput){
        for(E output : arrayOutput){
            out((String) output, true);
        }
    }

    public static void out(String output){
        System.out.println(output);
    }

    public static void out(String output, boolean newLine){
        if(newLine){
            System.out.println(output);
        }else{
            System.out.print(output);
        }
    }

// -----------------------------------------------------------------------------------------------------------
    
    public static <E> void out(E[] arrayOutput, String color){
        for(E output : arrayOutput){
            out((String) output, color, true);
        }
    }

    public static void out(String output, String color){
        System.out.println(color + output + "\u001b[0m");
    }

    public static void out(String output, String color, boolean newLine){
        if(newLine){
            System.out.println(color + output + "\u001b[0m");
        }else{
            System.out.print(color + output + "\u001b[0m");
        }
    }
}
