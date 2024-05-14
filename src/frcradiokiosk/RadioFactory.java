package frcradiokiosk;

import frcradiokiosk.DAP1522RevB.DAP1522revB;
import frcradiokiosk.openMesh.OM5P_AN;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadioFactory {
   private static RadioFactory instance;
   private RadioFactory.RadioOptions defaultRadioType = RadioFactory.RadioOptions.OM5P_AN;
   private RadioFactory.RadioOptions userRadioSelection = this.defaultRadioType;
   private FRCRadio.Mode radioMode = FRCRadio.Mode.BRIDGE;
   private ArrayList<RadioFactory.RadioFactoryChangeListener> listeners = new ArrayList<>();
   private static final Logger logger = LoggerFactory.getLogger(RadioFactory.class);

   public static RadioFactory getInstance() {
      if (instance == null) {
         instance = new RadioFactory();
         logger.info("New radio factory created");
      }

      return instance;
   }

   private RadioFactory() {
   }

   public void registerListener(RadioFactory.RadioFactoryChangeListener listener) {
      this.listeners.add(listener);
   }

   public void removeListener(RadioFactory.RadioFactoryChangeListener listener) {
      this.listeners.remove(listener);
   }

   private void updateListeners() {
      for (RadioFactory.RadioFactoryChangeListener listener : this.listeners) {
         logger.info("Updating factory change listener: " + listener.getClass().toString());
         listener.factoryParametersChanged(this.userRadioSelection, this.radioMode);
      }

      logger.info("Factory change listeners updated");
   }

   public RadioFactory.RadioOptions getDefaultRadioType() {
      return this.defaultRadioType;
   }

   public RadioFactory.RadioOptions getSelectedRadioType() {
      return this.userRadioSelection;
   }

   public void selectRadio(RadioFactory.RadioOptions radio) {
      this.userRadioSelection = radio;
      this.updateListeners();
      logger.info("Radio selection changed to {}", radio.toString());
   }

   public FRCRadio getRadio(int team, String key) {
      logger.info("Getting radio by team and key");
      return this.getRadio(team, String.valueOf(team), key, "");
   }

   public FRCRadio getRadio(int team, String key, String password) {
      logger.info("Getting radio by string, key and pass");
      return this.getRadio(team, String.valueOf(team), key, password);
   }

   public FRCRadio getRadio(int team, String ssid, String key, String password) {
      logger.info("Getting radio by string, key and pass");
      return this.getRadio(team, ssid, key, password, false);
   }

   public FRCRadio getRadio(int team, String ssid, String key, String password, boolean firewall) {
      logger.info("Getting radio with all params: " + this.userRadioSelection.toString());
      FRCRadio radio;
      switch (this.userRadioSelection) {
         case DAP1522_Rev_A:
            radio = new DAP1522(team, ssid, key, this.radioMode, password);
            break;
         case DAP1522_Rev_B:
            radio = new DAP1522revB(team, ssid, key, this.radioMode, password);
            break;
         case OM5P_AN:
            radio = new OM5P_AN(team, ssid, key, this.radioMode, password);
            break;
         default:
            radio = new OM5P_AN(team, ssid, key, this.radioMode, password);
      }

      radio.firewall = firewall;
      return radio;
   }

   public void setRadioMode(FRCRadio.Mode mode) {
      this.radioMode = mode;
      this.updateListeners();
      logger.info("Radio mode changed to {}", mode.getLongString());
   }

   public interface RadioFactoryChangeListener {
      void factoryParametersChanged(RadioFactory.RadioOptions var1, FRCRadio.Mode var2);
   }

   public static enum RadioOptions {
      DAP1522_Rev_A("DAP1522 RevA"),
      DAP1522_Rev_B("DAP1522 RevB"),
      OM5P_AN("OpenMesh");

      private String string;

      private RadioOptions(String string) {
         this.string = string;
      }

      @Override
      public String toString() {
         return this.string;
      }
   }
}
