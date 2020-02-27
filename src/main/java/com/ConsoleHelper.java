package main.java.com;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));


    public static void writeMessage(String message) {
        System.out.println(message);
    }

    public static String readString() {
        String inputString = null;
        try {
            inputString = reader.readLine();
        } catch (IOException e) {
            System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
            inputString = readString();
        }

        return inputString;
    }

    public static int readInt() {
        int inputNumber = 0;
        try {
            inputNumber = Integer.parseInt(readString());
        } catch (NumberFormatException e) {
            System.out.println("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
            inputNumber = readInt();
        }

        return inputNumber;
    }
}
