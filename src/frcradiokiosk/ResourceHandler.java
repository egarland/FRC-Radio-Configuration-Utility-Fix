package frcradiokiosk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceHandler {
   private static final Logger logger = LoggerFactory.getLogger(ResourceHandler.class);

   public void closeReader(Reader reader) {
      try {
         if (reader != null) {
            reader.close();
            logger.debug("Reader closed");
         }
      } catch (IOException var3) {
         logger.warn("Failed to close reader", var3);
      }
   }

   public void closeWriter(Writer writer) {
      try {
         if (writer != null) {
            writer.close();
            logger.debug("Writer closed");
         }
      } catch (IOException var3) {
         logger.warn("Failed to close writer", var3);
      }
   }

   public void closeOutputStream(OutputStream out) {
      try {
         if (out != null) {
            out.close();
            logger.debug("Output stream closed");
         }
      } catch (IOException var3) {
         logger.warn("Failed to close output stream", var3);
      }
   }

   public void closeInputStream(InputStream in) {
      try {
         if (in != null) {
            in.close();
            logger.debug("Input stream closed");
         }
      } catch (IOException var3) {
         logger.warn("Failed to close in stream", var3);
      }
   }

   public void closeProcess(Process p) {
      if (p != null) {
         p.destroy();
         logger.debug("Process closed");
      }
   }

   public void closeURLConnection(HttpURLConnection conn) {
      if (conn != null) {
         conn.disconnect();
         logger.debug("HTTP connection disconnected");
      }
   }

   public void closeDatagramSocket(DatagramSocket socket) {
      if (socket != null) {
         if (socket.isConnected()) {
            socket.disconnect();
         }

         socket.close();
         logger.debug("network socket closed");
      }
   }
}
