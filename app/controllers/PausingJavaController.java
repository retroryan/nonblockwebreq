package controllers;

import play.Logger;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.HashMap;
import java.util.Map;


public class PausingJavaController extends Controller {

    public static Result pause(long duration) {
        Logger.debug("java returning " + duration);
        F.Promise<Result> timeout = F.Promise.timeout((Result) ok(String.valueOf(duration)), duration);
        return async(timeout);
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
