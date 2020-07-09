package edu.pucmm.eict.jws;

import io.javalin.Javalin;
import io.javalin.core.util.RouteOverviewPlugin;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static j2html.TagCreator.*;
import static j2html.TagCreator.a;

public class Main {

    //Creando el repositorio de las sesiones recibidas.
    public static List<Session> usuariosConectados = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Hola Mundo en Javalin - Socket");
        //inicio del servidor.
        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.addStaticFiles("/publico");
            javalinConfig.registerPlugin(new RouteOverviewPlugin("/rutas")); //aplicando plugins de las rutas
        }).start(getHerokuAssignedPort());

        //
        app.get("/", ctx -> {
            String tramaHtml = html(
                    j2html.TagCreator.head(title("Ejemplo de WebSocket")),
                    body(
                            h1("Ejemplo de Ajax y WebSocket"),
                            h2(a("Ejemplo Polling").withHref("/ejemploPolling.html")),
                            h2(a("Ejemplo WebSocket").withHref("/ejemploWebSocket.html"))
                    )).render();
            ctx.html(tramaHtml);
        });

        /**
         * http://localhost:7000/polling
         */
        app.get("/polling", ctx -> {
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            ctx.result(""+format.format(new Date()));
        });

        /**
         * http://localhost:7000/enviarMensaje?mensaje=Hola Mundo
         */
        app.get("/enviarMensaje", ctx -> {
            String mensaje = ctx.queryParam("mensaje");
            enviarMensajeAClientesConectados(mensaje, "rojo");
            ctx.result("Enviando mensaje: "+mensaje);
        });

        /**
         * Filtro para activarse antes de la llamadas al contexto.
         */
        app.wsBefore("/mensajeServidor", wsHandler -> {
            System.out.println("Filtro para WS antes de la llamada ws");
            //ejecutar cualquier evento antes...
        });

        /**
         * Definición del WS en Javalin en contexto
         */
        app.ws("/mensajeServidor", ws -> {

            ws.onConnect(ctx -> {
                System.out.println("Conexión Iniciada - "+ctx.getSessionId());
                usuariosConectados.add(ctx.session);
            });

            ws.onMessage(ctx -> {
                //Puedo leer los header, parametros entre otros.
                ctx.headerMap();
                ctx.pathParamMap();
                ctx.queryParamMap();
                //
                System.out.println("Mensaje Recibido de "+ctx.getSessionId()+" ====== ");
                System.out.println("Mensaje: "+ctx.message());
                System.out.println("================================");
                //
                enviarMensajeAClientesConectados(ctx.message(), "azul");
            });

            ws.onBinaryMessage(ctx -> {
                System.out.println("Mensaje Recibido Binario "+ctx.getSessionId()+" ====== ");
                System.out.println("Mensaje: "+ctx.data().length);
                System.out.println("================================");
            });

            ws.onClose(ctx -> {
                System.out.println("Conexión Cerrada - "+ctx.getSessionId());
                usuariosConectados.remove(ctx.session);
            });
            ws.onError(ctx -> {
                System.out.println("Ocurrió un error en el WS");
            });
        });

        /**
         * Filtro para activarse despues de la llamadas al contexto.
         */
        app.wsAfter("/mensajeServidor", wsHandler -> {
            System.out.println("Filtro para WS despues de la llamada al WS");
            //ejecutar cualquier evento antes...
        });

    }

    /**
     * Permite enviar un mensaje al cliente.
     * Ver uso de la librería: https://j2html.com/
     * @param mensaje
     * @param color
     */
    public static void enviarMensajeAClientesConectados(String mensaje, String color){
        for(Session sesionConectada : usuariosConectados){
            try {
                sesionConectada.getRemote().sendString(p(mensaje).withClass(color).render());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Metodo para indicar el puerto en Heroku
     * @return
     */
    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 7000; //Retorna el puerto por defecto en caso de no estar en Heroku.
    }
}
