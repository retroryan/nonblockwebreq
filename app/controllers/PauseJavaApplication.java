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
import play.mvc.Security;

import java.util.List;
import java.util.concurrent.Callable;

import static akka.pattern.Patterns.ask;

public class PauseJavaApplication extends Controller {

    final static Form<TestParams> testParamsForm = form(TestParams.class);


    public static Result testPromise() {

        F.Promise<String> integerPromise = Akka.future(new Callable<String>() {
            @Override
            public String call() throws Exception {
                //if (1 == 2)
               //     throw new Exception("opps!");

                Call pauseCall = routes.PausingJavaController.pause(1);
                String url = "http://" + "localhost:9000" + pauseCall.url();

                String wsResults = WS.url(url)
                        .setQueryParameter("duration", String.valueOf(1))
                        .get().get().getBody(); // block here

                return wsResults;
            }
        });

        F.Function<String, Result> onSuccess = new F.Function<String, Result>() {
            @Override
            public Result apply(String wsResults) throws Throwable {
                return ok(wsResults);
            }
        };

        F.Function<Throwable, Result> onError = new F.Function<Throwable, Result>() {
            @Override
            public Result apply(Throwable throwable) throws Throwable {
                return ok(throwable.toString());
            }
        };
        return async(
                integerPromise
                        .map(onSuccess)
                        .recover(onError)
        );
    }

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

        F.Promise<List<WS.Response>> listPromise = F.Promise.waitAll(threePromise, onePromise, fourPromise);

        return async(
                listPromise.map(new F.Function<List<WS.Response>, Result>() {
                    @Override
                    public Result apply(List<WS.Response> responses) throws Throwable {
                        StringBuilder content = new StringBuilder();
                        for (WS.Response response : responses) {
                            content.append(response);
                        }
                        return ok(content.toString());
                    }
                }
                )
        );
    }

    private static String nrActorInstances = play.Play.application().configuration().getString("ws.number.actorinstances");

    //SmallestMailboxRouter    RoundRobinRouter
    private static ActorRef wsActorRouter = Akka.system()
            .actorOf(new Props(WSRequestActor.class)
                    .withRouter(new RoundRobinRouter(Integer.parseInt(nrActorInstances)))
                    .withDispatcher("my-balancing-dispatcher"));

    @Security.Authenticated
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
