package com.fgil55.weathergraph.util;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class SystemProperties {

    private static String GETPROP_EXECUTABLE_PATH = "/system/bin/getprop";
    private static String TAG = "WeatherGraph";

    public static Locale LOCALE = systemLocale();

    private static Locale systemLocale() {
        String country = StringUtils.trim(read("persist.sys.country")).toUpperCase();

        switch (country) {
            case "ES":
                return new Locale("es");
            case "FR":
                return Locale.FRENCH;
            case "DE":
                return Locale.GERMAN;
            case "IT":
                return Locale.ITALIAN;
            default:
                return Locale.ENGLISH;
        }
    }

    public static String read(String propName) {
        Process process = null;
        BufferedReader bufferedReader = null;

        try {
            process = new ProcessBuilder().command(GETPROP_EXECUTABLE_PATH, propName).redirectErrorStream(true).start();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = bufferedReader.readLine();
            if (line == null) {
                line = ""; //prop not set
            }
            Log.i(TAG, "read System Property: " + propName + "=" + line);
            return line;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read System Property " + propName, e);
            return "";
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }
}