package controllers;

import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


public class PausingJavaController extends Controller {

    public static Result pause(final int duration) {
        Logger.info("java pausing for  " + duration + " seconds");

        Callable<Result> callable = new Callable<Result>(){
            public Result call()  throws Exception {
                return ok("paused for " + String.valueOf(duration));
            }
        };

        F.Promise<Result> promiseOfInt = Akka.timeout(callable, Long.valueOf(duration), TimeUnit.SECONDS);

        return async(promiseOfInt);
    }

    public static String memoryOverload(int memoryFillSize) {
        Map<String, String> mapContainer = new HashMap<String, String>();

        String stringFiller = "stringFiller";
        String largeString = "start";

        for (int indx = 0; indx < memoryFillSize; indx++) {
            String newStringData = stringFiller + indx;
            largeString += newStringData;
            mapContainer.put(newStringData, newStringData);
            mapContainer.put(String.valueOf(indx), largeString);
        }

        String mapSize = String.valueOf(mapContainer.size());
        System.out.println("map size = " + mapSize);

        for (int indx = 0; indx < memoryFillSize; indx++) {
            String newStringData = stringFiller + indx;
            mapContainer.remove(newStringData);
            mapContainer.remove(String.valueOf(indx));
        }

        mapSize = String.valueOf(mapContainer.size());
        return mapSize;
    }
}
