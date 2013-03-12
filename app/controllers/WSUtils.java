package controllers;


import play.Logger;
import play.libs.F;
import play.libs.WS;
import play.mvc.Result;

import static play.mvc.Results.ok;

public class WSUtils {

    public static String getPartialAsyncResult(String pauseDuration, String url) {
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
        Logger.debug("content = " + content);

        return content;
    }
}
