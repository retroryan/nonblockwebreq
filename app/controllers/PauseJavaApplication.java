package controllers;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.RoundRobinRouter;
import akka.routing.SmallestMailboxRouter;
import models.TestParams;
import play.Logger;
import play.Play;
import play.api.mvc.Call;
import play.data.Form;
import play.libs.Akka;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Result;

import static akka.pattern.Patterns.ask;

public class PauseJavaApplication extends Controller {

    final static Form<TestParams> testParamsForm = form(TestParams.class);

    // this handler occupies a thread until completed
    // three web requests run in sequence, each uses an additional thread (using only one additional thread at a time
    public static Result sync() {

        String pauseCall = routes.PausingJavaController.pause(3).absoluteURL(request());

        String three = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(1))
                .get().get().getBody(); // block here
        String one = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(1))
                .get().get().getBody(); // block here
        String four = WS.url(pauseCall)
                .setQueryParameter("duration", String.valueOf(1))
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

        // get from test params does not work in Play 2.0.4 ?
        // TestParams testParams = filledTestParams.get();
        String pauseDuration = filledTestParams.field("pauseDuration").value();
        String host = filledTestParams.field("host").value();

        Call pauseCall = routes.PausingJavaController.pause(Integer.parseInt(pauseDuration));
        String url = "http://" + host + pauseCall.url();
        Logger.debug("THE URL: " + url);

        F.Promise<WS.Response> threePromise = WS.url(url)
                .setQueryParameter("duration", pauseDuration)
                .get(); // schedule now

        F.Promise<WS.Response> onePromise = WS.url(url)
                .setQueryParameter("duration", pauseDuration)
                .get(); // schedule now

        F.Promise<WS.Response> fourPromise = WS.url(url)
                .setQueryParameter("duration", pauseDuration)
                .get(); // schedule now

        // order doesn't matter
        String three = threePromise.get().getBody();
        String one = onePromise.get().getBody();
        String four = fourPromise.get().getBody();

        String content = one + three + four;
        //Logger.debug("content = " + content);

        return ok(content);
    }

    // this handler only occupies a thread when active
    // three web requests run in parallel, when active the occupy a thread
    public static Result fullAsyncJava() {
        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        // get from test params does not work in Play 2.0.4 ?
        // TestParams testParams = filledTestParams.get();
        String pauseDuration = filledTestParams.field("pauseDuration").value();
        String host = filledTestParams.field("host").value();

        Call pauseCall = routes.PausingJavaController.pause(Integer.parseInt(pauseDuration));
        String url = "http://" + host + pauseCall.url();
        Logger.debug("URL: " + url);

        final F.Promise<WS.Response> threePromise = WS.url(url)
                .setQueryParameter("duration", pauseDuration)
                .get(); // schedule now

        final F.Promise<WS.Response> onePromise = WS.url(url)
                .setQueryParameter("duration", pauseDuration)
                .get(); // schedule now

        final F.Promise<WS.Response> fourPromise = WS.url(url)
                .setQueryParameter("duration", pauseDuration)
                .get(); // schedule now

        return async(
                composePromises(threePromise, onePromise, fourPromise)
        );
    }

    private static F.Promise<Result> composePromises(F.Promise<WS.Response> threePromise, final F.Promise<WS.Response> onePromise, final F.Promise<WS.Response> fourPromise) {
        return threePromise.flatMap(
                new F.Function<WS.Response, F.Promise<Result>>() {
                    public F.Promise<Result> apply(final WS.Response threeResponse) {
                        return onePromise.flatMap(
                                new F.Function<WS.Response, F.Promise<Result>>() {
                                    public F.Promise<Result> apply(final WS.Response oneResponse) {
                                        return fourPromise.map(
                                                new F.Function<WS.Response, Result>() {
                                                    public Result apply(final WS.Response fourResponse) {
                                                        String content = oneResponse.getBody() + threeResponse.getBody() + fourResponse.getBody();
                                                        Logger.info("content = " + content);
                                                        return ok(content);
                                                    }
                                                }
                                        );
                                    }
                                }
                        );
                    }
                }
        );
    }

    private static String nrActorInstances = play.Play.application().configuration().getString("ws.number.actorinstances");

    //SmallestMailboxRouter    RoundRobinRouter
    private static ActorRef wsActorRouter = Akka.system()
            .actorOf(new Props(WSRequestActor.class)
            .withRouter(new SmallestMailboxRouter(Integer.parseInt(nrActorInstances)))
            .withDispatcher("my-balancing-dispatcher"));

    public static Result actorRequest() {
        Form<TestParams> filledTestParams = testParamsForm.bindFromRequest();
        if (filledTestParams.hasErrors()) {
            return badRequest("Bad test params");
        }
        // get from test params does not work in Play 2.0.4 ?
        // TestParams testParams = filledTestParams.get();
        String pauseDuration = filledTestParams.field("pauseDuration").value();
        int duration = Integer.parseInt(pauseDuration);
        String host = filledTestParams.field("host").value();

        TestParams testParams = new TestParams(duration, 0, host);
        //ActorRef wsReqActor = Akka.system().actorOf(new Props(WSRequestActor.class));


        return async(
                Akka.asPromise(ask(wsActorRouter, testParams, 5000)).map(
                        new F.Function<Object, Result>() {
                            public Result apply(Object response) {
                                return ok(response.toString());
                            }
                        }
                )
        );
    }


}
