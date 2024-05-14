package frcradiokiosk;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadioConnection {
   private URL siteUrl = null;
   private HttpURLConnection conn = null;
   private DataOutputStream out = null;
   private BufferedReader reader = null;
   private static final Logger logger = LoggerFactory.getLogger(RadioConnection.class);
   private static final ResourceHandler handler = new ResourceHandler();
   private static XMLOutputter xmlOutput = null;

   public static RadioConnection createOutputConnection(String url, boolean xml) throws RadioException {
      logger.debug("Initializing output stream");

      try {
         RadioConnection rc = new RadioConnection();
         if (xml) {
            rc.setupXmlOutput(url);
         } else {
            rc.setupStringOutput(url);
         }

         return rc;
      } catch (RadioException var31) {
         throw new RadioException("Failed to initialize output connection for " + url + " webpage", var31);
      }
   }

   public static RadioConnection createInputConnection(String url, int timeout) throws RadioException {
      logger.debug("Initializing output stream");

      try {
         RadioConnection rc = new RadioConnection();
         rc.setupInput(url, timeout);
         return rc;
      } catch (RadioException var31) {
         throw new RadioException("Failed to initialize input connection for " + url + " webpage", var31);
      }
   }

   private RadioConnection() {
      if (xmlOutput == null) {
         xmlOutput = new XMLOutputter(Format.getPrettyFormat());
      }
   }

   private void setupXmlOutput(String url) throws RadioException {
      try {
         this.siteUrl = new URL(url);
         this.conn = (HttpURLConnection)this.siteUrl.openConnection();
         this.conn.setReadTimeout(30000);
         this.conn.setRequestMethod("POST");
         this.conn.setDoOutput(true);
         this.conn.setDoInput(true);
         this.conn.setRequestProperty("Content-Type", "text/xml");
         this.conn.setRequestProperty("Cookie", "uid=l012sUATwq");
         this.out = new DataOutputStream(this.conn.getOutputStream());
      } catch (IOException var3) {
         this.close();
         throw new RadioException("Failed to initialize output connection for " + url + " webpage", var3);
      }
   }

   private void setupStringOutput(String url) throws RadioException {
      try {
         this.siteUrl = new URL(url);
         this.conn = (HttpURLConnection)this.siteUrl.openConnection();
         this.conn.setReadTimeout(30000);
         this.conn.setRequestMethod("POST");
         this.conn.setDoOutput(true);
         this.conn.setDoInput(true);
         this.conn.setRequestProperty("Cookie", "uid=l012sUATwq");
         this.out = new DataOutputStream(this.conn.getOutputStream());
      } catch (IOException var3) {
         this.close();
         throw new RadioException("Failed to initialize output connection for " + url + " webpage", var3);
      }
   }

   private void setupInput(String url, int timeout) throws RadioException {
      try {
         this.siteUrl = new URL(url);
         this.conn = (HttpURLConnection)this.siteUrl.openConnection();
         this.conn.setRequestMethod("GET");
         this.conn.setRequestProperty("Cookie", "uid=l012sUATwq");
         if (timeout > 0) {
            this.conn.setReadTimeout(timeout * 1000);
         }

         logger.debug("connection intialized, getting response");
         this.reader = new BufferedReader(new InputStreamReader(this.conn.getInputStream()));
      } catch (IOException var4) {
         this.close();
         throw new RadioException("Failed to initialize input connection for " + url + " webpage", var4);
      }
   }

   public URL getUrl() {
      return this.siteUrl;
   }

   public String getStringUrl() {
      return this.siteUrl.toString();
   }

   public HttpURLConnection getHTTPConnection() {
      return this.conn;
   }

   public DataOutputStream getOutputStream() {
      return this.out;
   }

   public BufferedReader getReader() {
      return this.reader;
   }

   public void close() {
      handler.closeOutputStream(this.out);
      handler.closeReader(this.reader);
      handler.closeURLConnection(this.conn);
   }

   public String getPage(boolean ignoreTimeout) throws RadioException {
      logger.debug("getting page {}", this.getStringUrl());
      StringBuilder data = new StringBuilder();

      Object var4;
      try {
         String line = "";

         while ((line = this.getReader().readLine()) != null) {
            data.append(line);
            logger.trace(line);
         }

         logger.debug("Page received");
         return data.toString();
      } catch (SocketTimeoutException var101) {
         if (!ignoreTimeout) {
            throw new RadioException("Timed out while getting " + this.getStringUrl() + " webpage", var101);
         }

         logger.debug("Timed out; OK");
         var4 = null;
      } catch (IOException var11) {
         throw new RadioException("Failed to get " + this.getStringUrl() + " webpage", var11);
      } finally {
         this.close();
      }

      return (String)var4;
   }

   public Document pushData(Document document) throws RadioException {
      Document e;
      try {
         logger.debug("Pushing data to {}", this.getStringUrl());
         logger.trace(xmlOutput.outputString(document));
         xmlOutput.output(document, this.getOutputStream());
         logger.debug("settings pushed to radio");
         e = this.getXMLresponse();
      } catch (IOException var7) {
         throw new RadioException("failed to push xml data to " + this.getStringUrl());
      } finally {
         this.close();
      }

      return e;
   }

   public Document pushData(String data) throws RadioException {
      Document e;
      try {
         logger.debug("Pushing data to {}", this.getStringUrl());
         logger.trace(data);
         this.getOutputStream().writeBytes(data);
         this.getOutputStream().flush();
         logger.debug("settings pushed to radio");
         e = this.getXMLresponse();
      } catch (IOException var7) {
         throw new RadioException("Failed to post data to " + this.getStringUrl() + " webpage", var7);
      } finally {
         this.close();
      }

      return e;
   }

   private Document getXMLresponse() throws RadioException {
      try {
         logger.debug("getting response...");
         this.reader = new BufferedReader(new InputStreamReader(this.conn.getInputStream()));
         SAXBuilder builder = new SAXBuilder();
         Document response = builder.build(this.getReader());
         logger.debug("response received");
         logger.trace(xmlOutput.outputString(response));
         return response;
      } catch (JDOMException var3) {
         throw new RadioException("Failed to get XML response");
      } catch (IOException var41) {
         throw new RadioException("Failed to get XML response", var41);
      }
   }
}
