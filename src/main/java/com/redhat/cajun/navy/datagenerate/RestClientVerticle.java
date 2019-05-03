package com.redhat.cajun.navy.datagenerate;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;


public class RestClientVerticle extends AbstractVerticle {

    private static Logger log = LoggerFactory.getLogger(RestClientVerticle.class);

    private WebClient client;

    private String incidentServiceHost;

    private int incidentServicePort;

    private String incidentServiceCreatePath;

    private String incidentServiceResetPath;

    private String responderServiceHost;

    private int responderServicePort;

    private String responderServiceResetPath;

    private String missionServiceHost;

    private int missionServicePort;

    private String missionServiceResetPath;

    private String incidentPriorityServiceHost;

    private int incidentPriorityServicePort;

    private String incidentPriorityServiceResetPath;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        vertx.eventBus().consumer("rest-client-queue", this::onMessage);
        client = WebClient.create(vertx);
        incidentServiceHost = config().getString("incident.service.host");
        incidentServicePort = config().getInteger("incident.service.port");
        incidentServiceResetPath = config().getString("incident.service.path.reset");
        incidentServiceCreatePath = config().getString("incident.service.path.create");
        responderServiceHost = config().getString("responder.service.host");
        responderServicePort = config().getInteger("responder.service.port");
        responderServiceResetPath = config().getString("responder.service.path.reset");
        missionServiceHost = config().getString("mission.service.host");
        missionServicePort = config().getInteger("mission.service.port");
        missionServiceResetPath = config().getString("mission.service.path.reset");
        incidentPriorityServiceHost = config().getString("incidentpriority.service.host");
        incidentPriorityServicePort = config().getInteger("incidentpriority.service.port");
        incidentPriorityServiceResetPath = config().getString("incidentpriority.service.path.reset");

        startFuture.complete();
    }

    public enum ErrorCodes {
        NO_ACTION_SPECIFIED,
        BAD_ACTION
    }


    private void onMessage(Message<JsonObject> message) {

        String action = message.headers().get("action");
        switch (action) {
            case "create-incident":
                createIncident(message);
                break;

            case "reset-incidents":
                resetIncidents();
                break;

            case "reset-responders":
                resetResponders(message);
                break;

            case "reset-missions":
                resetMissions();
                break;

            case "reset-incident-priority":
                resetIncidentPriority();
                break;

            default:
                message.fail(ErrorCodes.BAD_ACTION.ordinal(),"Message failed");
        }
    }

    private void resetResponders(Message<JsonObject> message) {
        client.post(responderServicePort, responderServiceHost, responderServiceResetPath)
                .putHeader("Content-Type", "application/json")
                .sendBuffer(message.body().getJsonArray("responders").toBuffer(), ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        log.info("Init responders: HTTP response status " + response.statusCode());
                    } else {
                        log.error("Init responders failed.", ar.cause());
                    }
                });
    }


    private void createIncident(Message<JsonObject> message) {
        client.post(incidentServicePort, incidentServiceHost, incidentServiceCreatePath)
                .sendJsonObject(message.body(), ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        log.info("Create incident for" + message.body().getString("victimName") + ": HTTP response status " + response.statusCode());
                    }
                    else log.error("Create incident for" + message.body().getString("victimName") + " failed.", ar.cause());

                });
    }

    private void resetIncidents() {
        client.post(incidentServicePort, incidentServiceHost, incidentServiceResetPath)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        log.info("Reset incidents: HTTP response status " + response.statusCode());
                    } else {
                        log.error("Reset incidents failed.", ar.cause());
                    }
                });
    }

    private void resetMissions() {
        client.get(missionServicePort, missionServiceHost, missionServiceResetPath)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        log.info("Reset missions: HTTP response status " + response.statusCode());
                    } else {
                        log.error("Reset missions failed.", ar.cause());
                    }
                });
    }

    private void resetIncidentPriority() {
        client.post(incidentPriorityServicePort, incidentPriorityServiceHost, incidentPriorityServiceResetPath)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        log.info("Reset incident priority: HTTP response status " + response.statusCode());
                    } else {
                        log.error("Reset incident priority failed.", ar.cause());
                    }
                });
    }
}
