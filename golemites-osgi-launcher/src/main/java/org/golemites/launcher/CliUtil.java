package org.golemites.launcher;

import java.io.PrintStream;

public class CliUtil {
    public static final String blue = "\u001B[36m";
    public static final String green = "\u001B[92m";
    public static final String red = "\u001B[91m";
    public static final String end = "\u001B[0m\u001B[0m";

    public static String good(String message) {
        return green + message + end;
    }

    public static String bad(String message) {
        return red + message + end;
    }

    public static String info(String message) {
        return blue + message + end;
    }

    public static void printSuccess(String msg) {
        System.out.println(  good("SUCCESS") + " : " +msg);
    }

    public static void printFailure(String msg) {
        printFailure( System.out, msg);
    }

    public static void printFailure(PrintStream ps, String msg) {
        ps.println( bad("FAILED") + "  : " +msg);
    }

    public static void printWarning(String msg) {
        System.out.println( bad("WARNING") + "  : " +msg);
    }

    public static void printInfo(String msg) {
        System.out.println( good("INFO") + "  : " +msg);
    }
}
