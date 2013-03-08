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

    public static Result pause(final long duration) {
        Logger.debug("java pausing for  " + duration + " seconds");

        Callable<Long> callable = new Callable<Long>() {
            public Long call() {
                return duration;
            }
        };

        F.Promise<Long> promiseOfInt = Akka.timeout(callable,duration, TimeUnit.SECONDS);

        return async(
                promiseOfInt.map(
                        new F.Function<Long, Result>() {
                            public Result apply(Long i) {
                                return ok(i.toString());
                            }
                        }
                )
        );
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
