package controllers;

import models.TestParams;
import play.Play;
import play.api.mvc.Call;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static play.data.Form.form;

public class PauseJavaApplication extends Controller {

    final static Form<TestParams> testParamsForm = form(TestParams.class);

    // this handler occupies a thread until completed
    // three web requests run in sequence, each uses an additional thread (using only one additional thread at a time
    public static Result sync() {
        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        TestParams testParams = filledTestParams.get();

        String pauseCall = routes.PauseJavaApplication.pause(testParams.pauseDuration, testParams.memoryFillSize).absoluteURL(request());

        String three = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get().get().getBody(); // block here
        String one = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get().get().getBody(); // block here
        String four = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get().get().getBody(); // block here

        return ok(one + three + four);
    }

    // this handler occupies a thread until completed
    // three web requests run in parallel, when active they occupy a thread
    public static Result partialAsync() {

        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        TestParams testParams = filledTestParams.get();

        String pauseCall = routes.PauseJavaApplication.pause(testParams.pauseDuration, testParams.memoryFillSize).absoluteURL(request());

        F.Promise<WS.Response> threePromise = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .setTimeout(500000)
                .get(); // schedule now

        F.Promise<WS.Response> onePromise = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .setTimeout(500000)
                .get(); // schedule now

        F.Promise<WS.Response> fourPromise = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .setTimeout(500000)
                .get(); // schedule now

        // order doesn't matter
        String three = threePromise.get().getBody();
        String one = onePromise.get().getBody();
        String four = fourPromise.get().getBody();

        return ok(one + three + four);
    }

    // this handler only occupies a thread when active
    // three web requests run in parallel, when active the occupy a thread
    public static Result fullAsync() {
        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        TestParams testParams = filledTestParams.get();

        String pauseCall = routes.PauseJavaApplication.pause(testParams.pauseDuration, testParams.memoryFillSize).absoluteURL(request());

        final F.Promise<WS.Response> threePromise = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get(); // schedule now
        final F.Promise<WS.Response> onePromise = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get(); // schedule now
        final F.Promise<WS.Response> fourPromise =WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get(); // schedule now

        return async(
                threePromise.flatMap(
                        new F.Function<WS.Response, F.Promise<Result>>() {
                            public F.Promise<Result> apply(final WS.Response threeResponse) {
                                return onePromise.flatMap(
                                        new F.Function<WS.Response, F.Promise<Result>>() {
                                            public F.Promise<Result> apply(final WS.Response oneResponse) {
                                                return fourPromise.map(
                                                        new F.Function<WS.Response, Result>() {
                                                            public Result apply(final WS.Response fourResponse) {
                                                                return ok(oneResponse.getBody() + threeResponse.getBody() + fourResponse.getBody());
                                                            }
                                                        }
                                                );
                                            }
                                        }
                                );
                            }
                        }


                )
        );
    }


//    public static long fixedDuration = 1;

    public static Result pause(long duration, int memoryFillSize) {
      //  F.Promise<Result> timeout = F.Promise.timeout((Result) ok(memoryOverload(memoryFillSize)), duration);

      //  return async(timeout);

       return ok(memoryOverload(memoryFillSize));
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
            mapContainer.remove(indx);
        }

        mapSize = String.valueOf(mapContainer.size());
        return mapSize;
    }

}
