/*
 * Copyright 2020 (c) OK
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.mail.polis;

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple console client to {@link DAO}.
 *
 * @author Vadim Tsesko
 */
public final class Client {

  private static final Logger LOG = LoggerFactory.getLogger(Client.class);
  private static final String DATA = "data";

  private Client() {
    // Not instantiable
  }

  private static ByteBuffer from(final String value) {
    return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String from(final ByteBuffer value) {
    final byte[] bytes = new byte[value.remaining()];
    value.get(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Provides console to temporary DB.
   */
  public static void main(final String[] args) throws IOException {
    final File data = new File(DATA);
    if (!data.exists() && !data.mkdir()) {
      throw new IOException("Can't create directory: " + data);
    }
    if (!data.isDirectory()) {
      throw new IOException("Not directory: " + data);
    }

    LOG.info("Storing data in {}", data.getAbsolutePath());
    final DAO dao = DAOFactory.create(data);
    final String pkg = dao.getClass().getPackage().toString();
    LOG.info(
        "Welcome to " + pkg.substring(pkg.lastIndexOf('.') + 1) + " Key-Value DAO!"
            + "\nSupported commands:"
            + "\n\tget <key>"
            + "\n\tput <key> <value>"
            + "\n\tremove <key>"
            + "\n\tquit");

    try (var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      String line;
      while (!"quit".equals(line = reader.readLine())) {
        if (line.isEmpty()) {
          continue;
        }

        final Iterator<String> tokens = Splitter.on(' ').split(line).iterator();
        final String cmd = tokens.next();
        final ByteBuffer key = ByteBuffer.wrap(tokens.next().getBytes(StandardCharsets.UTF_8));

        switch (cmd) {
          case "get" -> {
            try {
              LOG.info(from(dao.get(key)));
            } catch (NoSuchElementException e) {
              LOG.warn("absent");
            } catch (IOException e) {
              LOG.error("Can't extract key: " + key, e);
            }
          }

          case "put" -> dao.upsert(key, from(tokens.next()));

          case "remove" -> dao.remove(key);

          default -> LOG.error("Unsupported command: {}", cmd);
        }
      }
    } finally {
      dao.close();
    }
  }
}
