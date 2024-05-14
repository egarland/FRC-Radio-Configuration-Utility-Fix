package frcradiokiosk;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadioTelnetConnection {
   private TelnetClient telnet = new TelnetClient();
   private InputStream in;
   private PrintStream out;
   private static final Logger logger = LoggerFactory.getLogger(RadioTelnetConnection.class);

   public void connect(String target) throws RadioException {
      try {
         this.telnet.connect(target);
         this.in = this.telnet.getInputStream();
         this.out = new PrintStream(this.telnet.getOutputStream());
      } catch (SocketException var3) {
         throw new RadioException("Failed to connect to radio", var3);
      } catch (IOException var4) {
         throw new RadioException("Failed to connect to radio", var4);
      }
   }

   public void close() {
      try {
         this.telnet.disconnect();
      } catch (IOException var2) {
         logger.warn("Failed to close connection with radio", var2);
      }
   }

   public void writeSettings(List<String> settings, String command, String promptToken) throws RadioException {
      try {
         logger.debug("writing settings");

         for (String setting : settings) {
            this.write(command + " set " + setting);
            this.readUntil(promptToken);
         }

         this.write(command + " commit");
         this.readUntil(promptToken);
      } catch (RadioException var6) {
         throw new RadioException("Failed to write settings to radio", var6.getUserMessage(), var6);
      }
   }

   public void write(String value) {
      logger.trace("writing {}", value);
      this.out.println(value);
      this.out.flush();
   }

   public String readUntil(String prompt) throws RadioException {
      return this.readUntil(new String[]{prompt});
   }

   public String readUntil(String[] prompts) throws RadioException {
      logger.debug("reading until {} ", prompts);

      try {
         StringBuilder sb = new StringBuilder();
         this.telnet.setSoTimeout(5000);

         while (true) {
            char ch = (char)this.in.read();
            System.out.print(String.valueOf(ch));
            sb.append(ch);

            for (String prompt : prompts) {
               if (ch == prompt.charAt(prompt.length() - 1) && sb.toString().endsWith(prompt)) {
                  return sb.toString();
               }
            }
         }
      } catch (SocketTimeoutException var8) {
         throw new RadioException("Telnet read timeout", "A radio communication error occurred", var8);
      } catch (IOException var91) {
         throw new RadioException("Failed to read telnet data", "A radio communication error occurred", var91);
      }
   }
}
