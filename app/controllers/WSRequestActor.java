package controllers;

import akka.actor.UntypedActor;
import models.TestParams;
import play.Logger;
import play.api.mvc.Call;
import play.libs.WS;

public class WSRequestActor extends UntypedActor {


    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof TestParams) {
            TestParams testParams = (TestParams) message;
            Call pauseCall = routes.PausingJavaController.pause(testParams.pauseDuration);
            String url = "http://" + testParams.host + pauseCall.url();
           // Logger.info("URL: " + url);

            //execute the WS request as a fulling blocking call,
            // since an actor is already in a separate thread.
/*
            String result = WS.url(url)
                    .setQueryParameter("duration", String.valueOf(testParams.pauseDuration))
                    .get().get().getBody();
*/


            Logger.info("finished ws call with result of : " + 1);
            getSender().tell(" actor return msg: " + 1);

        }
    }
}
