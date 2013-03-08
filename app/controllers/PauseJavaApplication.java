package controllers;

import models.TestParams;
import play.Logger;
import play.api.mvc.Call;
import play.data.Form;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Result;


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

        String pauseCall = routes.PausingJavaController.pause(3).absoluteURL(request());

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
    public static Result partialAsyncJavaPause() {

        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        TestParams testParams = filledTestParams.get();
        System.out.println("testParams = " + testParams.pauseDuration);
        System.out.println("testParams = " + testParams.host);

        Call pauseCall = routes.PausingJavaController.pause(testParams.pauseDuration);
        String url = "http://" + testParams.host + pauseCall.url();
        Logger.debug("url = " + url);

        return getPartialAsyncResult(url);
    }

    // this handler occupies a thread until completed
    // three web requests run in parallel, when active they occupy a thread
    public static Result partialAsyncScalaPause() {

        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        TestParams testParams = filledTestParams.get();

        Call pauseCall = routes.PausingController.pause(testParams.pauseDuration);
        String url = "http://" + testParams.host + pauseCall.url();

        return getPartialAsyncResult(url);
    }


    private static Result getPartialAsyncResult(String url) {
        F.Promise<WS.Response> threePromise = WS.url(url)
                .setQueryParameter("duration", "3")
                .get(); // schedule now

        F.Promise<WS.Response> onePromise = WS.url(url)
                .setQueryParameter("duration", "1")
                .get(); // schedule now

        F.Promise<WS.Response> fourPromise = WS.url(url)
                .setQueryParameter("duration", "4")
                .get(); // schedule now

        // order doesn't matter
        String three = threePromise.get().getBody();
        String one = onePromise.get().getBody();
        String four = fourPromise.get().getBody();

        String content = one + three + four;
        System.out.println("content = " + content);

        return ok(content);
    }


    // this handler only occupies a thread when active
    // three web requests run in parallel, when active the occupy a thread
    public static Result fullAsync() {
        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        TestParams testParams = filledTestParams.get();

        String pauseCall = routes.PausingJavaController.pause(3).absoluteURL(request());

        final F.Promise<WS.Response> threePromise = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get(); // schedule now
        final F.Promise<WS.Response> onePromise = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                .setQueryParameter("memoryFillSize", String.valueOf(testParams.memoryFillSize))
                .get(); // schedule now
        final F.Promise<WS.Response> fourPromise = WS.url(pauseCall)
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


}
