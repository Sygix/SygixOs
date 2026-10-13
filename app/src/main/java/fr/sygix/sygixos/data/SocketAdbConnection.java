/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data;

import dadb.AdbConnection;
import dadb.AdbKeyPair;
import dadb.AdbShellResponse;
import dadb.AdbShellStream;
import java.io.IOException;
import java.net.Socket;

final class SocketAdbConnection implements AutoCloseable {
    private final AdbConnection connection;

    private SocketAdbConnection(AdbConnection connection) {
        this.connection = connection;
    }

    static SocketAdbConnection connect(Socket socket, AdbKeyPair keyPair) {
        return new SocketAdbConnection(AdbConnection.Companion.connect(socket, keyPair, 10_000L));
    }

    AdbShellResponse shell(String command) throws IOException {
        try (AdbShellStream stream = new AdbShellStream(connection.open("shell,v2,raw:" + command))) {
            return stream.readAll();
        }
    }

    @Override
    public void close() {
        connection.close();
    }
}
