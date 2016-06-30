package gateways.configuration;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import models.exceptions.BadRequest;
import models.exceptions.PostgreSQLException;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import akka.util.*;
import interactors.LocationProxyRule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorSystem;

public class Global extends GlobalSettings {
	private static final String appName = "ls";

	@Override
	public void onStart(Application app) {
		Logger.info(appName + " has started");
		
		Akka.system().scheduler().scheduleOnce(
			    Duration.create(0, TimeUnit.MILLISECONDS),
                new Runnable() {
                    @Override
                    public void run() {
                        Logger.info("ON START ---    " + System.currentTimeMillis());
                        JPA.withTransaction(() -> {
                        	LocationProxyRule.updateCache();
                        });
                    }
                },
                Akka.system().dispatcher()
        );
	}

	@Override
	public void onStop(Application app) {
		Logger.info(appName + " shutdown...");
	}

	/**
	 * Play 2.3.9 didn't catch Throwable and crashed so we catch them ourselves.
	 */
	@Override
	public Action<Void> onRequest(Request request, Method actionMethod) {
		return new Action.Simple() {
			public F.Promise<Result> call(Context ctx) throws Throwable {
				try {
					return delegate.call(ctx);
				} catch (BadRequest e) {
					final Status status = badRequest(toErrorMessageInJson(e));
					return Promise.<Result>pure(status);
				} catch (PostgreSQLException e) {
					final Status status = forbidden(toErrorMessageInJson(e));
					return Promise.<Result>pure(status);
				}catch (RuntimeException|Error e) {
					throw e;
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}

			private JsonNode toErrorMessageInJson(Exception e) {
				return Json.toJson(toErrorMessage(e));
			}

			private ErrorMessage toErrorMessage(Exception e) {
				ErrorMessage em = new ErrorMessage();
				em.userMessage = e.getMessage();
				em.type = e.getClass().getSimpleName();
				verbose(e, em);
				return em;
			}

			private void verbose(Exception e, ErrorMessage em) {
				if (!Play.isProd()){
					em.stackTrace = e.getStackTrace();
					em.cause = e.getCause();
				}
			}
		};
	}
}

@JsonInclude(Include.NON_NULL)
class ErrorMessage {
	public String userMessage;
	public Object develperClue;
	public String type;
	public Throwable cause;
	public StackTraceElement[] stackTrace;
}