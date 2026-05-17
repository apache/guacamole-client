/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Comprueba en paralelo si un conjunto de hosts:puerto responden via TCP.
 *
 * Cada prueba abre un socket TCP con un timeout de 1 segundo. Si la conexion
 * se establece, el host se considera alcanzable (reachable = true). Si falla
 * o expira el timeout, se considera inalcanzable (reachable = false).
 *
 * El probe NO verifica que el servicio Guacamole funcione correctamente, solo
 * que el puerto este abierto. Esto es suficiente para saber si la VM esta
 * encendida, ya que Windows y Linux dejan el puerto RDP/SSH abierto cuando
 * estan activos (a diferencia de ICMP que suele estar bloqueado en Windows).
 */
public class ReachabilityProbe {

    /** Numero maximo de sondas TCP en paralelo. */
    private static final int THREAD_POOL_SIZE = 16;

    /**
     * Timeout en milisegundos para cada intento de conexion TCP.
     * 1 segundo es suficiente para LAN local; evita bloquear la UI si una VM
     * tarda en responder o esta apagada.
     */
    private static final int CONNECT_TIMEOUT_MS = 1000;

    /**
     * Representa un destino a sondear: el ID de la conexion Guacamole,
     * el hostname o IP del host, y el puerto TCP.
     */
    public static class Target {

        /** Identificador de la conexion Guacamole (clave en el resultado del probe). */
        public final String id;

        /** Hostname o IP del host a sondear. */
        public final String host;

        /** Puerto TCP a sondear (1-65535). */
        public final int port;

        /**
         * Crea un nuevo Target.
         *
         * @param id   Identificador de la conexion Guacamole.
         * @param host Hostname o IP del host destino.
         * @param port Puerto TCP a sondear (1-65535).
         */
        public Target(String id, String host, int port) {
            this.id   = id;
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Sondea en paralelo todos los targets de la lista via TCP.
     *
     * Usa un ExecutorService con pool fijo de hasta THREAD_POOL_SIZE hilos.
     * Cada hilo intenta abrir un socket TCP al host:port del target con un
     * timeout de CONNECT_TIMEOUT_MS ms. El resultado se recoge con un timeout
     * de CONNECT_TIMEOUT_MS + 500 ms para dar margen a la Future.
     *
     * @param targets Lista de targets a sondear. Puede ser nula o vacia.
     * @return Mapa de connectionId (String) a boolean (true = alcanzable via TCP).
     */
    public static Map<String, Boolean> probe(List<Target> targets) {

        Map<String, Boolean> results = new HashMap<>();

        if (targets == null || targets.isEmpty())
            return results;

        // Pool acotado para no saturar el servidor con demasiados hilos
        int poolSize = Math.min(THREAD_POOL_SIZE, targets.size());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        // Lanzar una Future<Boolean> por cada target
        Map<String, Future<Boolean>> futures = new HashMap<>();
        for (Target target : targets) {
            final String id   = target.id;
            final String host = target.host;
            final int    port = target.port;

            futures.put(id, executor.submit(() -> {
                // Intentar conexion TCP; Socket se cierra automaticamente al salir del try-with-resources
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                    return true;
                } catch (IOException e) {
                    // Conexion rechazada, timeout o host desconocido -> no alcanzable
                    return false;
                }
            }));
        }

        // Cerrar el pool: no acepta mas tareas, pero espera a que las actuales terminen
        executor.shutdown();

        // Recoger resultados; si una Future tarda demasiado, asumir no alcanzable
        for (Map.Entry<String, Future<Boolean>> entry : futures.entrySet()) {
            try {
                Boolean reachable = entry.getValue().get(
                    CONNECT_TIMEOUT_MS + 500L, TimeUnit.MILLISECONDS);
                results.put(entry.getKey(), reachable != null && reachable);
            } catch (Exception e) {
                // Timeout de la Future, InterruptedException o error inesperado
                results.put(entry.getKey(), false);
            }
        }

        return results;
    }

}
