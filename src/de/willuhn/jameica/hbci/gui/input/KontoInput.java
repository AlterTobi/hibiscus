/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.gui.input;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.kapott.hbci.manager.HBCIUtils;

import de.willuhn.datasource.BeanUtil;
import de.willuhn.datasource.GenericObject;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.gui.filter.KontoFilter;
import de.willuhn.jameica.hbci.messaging.SaldoMessage;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.server.KontoUtil;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;

/**
 * Autosuggest-Feld zur Eingabe/Auswahl eines Kontos.
 */
public class KontoInput extends SelectInput
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getI18N();
  private final static de.willuhn.jameica.system.Settings settings = new de.willuhn.jameica.system.Settings(KontoInput.class);
  
  private static List<String> groups = null;
  
  private KontoListener listener = null;
  private String token = null;
  private Control control = null;

  private MessageConsumer mc = new SaldoMessageConsumer();
  
  /**
   * ct.
   * @param konto ausgewaehltes Konto.
   * @param filter optionaler Konto-Filter.
   * @throws RemoteException
   */
  public KontoInput(Konto konto, KontoFilter filter) throws RemoteException
  {
    super(init(filter),konto);
    setName(i18n.tr("Konto"));
    setPleaseChoose(i18n.tr("Bitte w�hlen..."));
    this.setComment("");
    
    if (konto == null)
    {
      konto = Settings.getDefaultKonto();
      if (konto != null)
        setPreselected(konto);
    }
    this.listener = new KontoListener();
    this.addListener(this.listener);

    // einmal ausloesen
    this.listener.handleEvent(null);
  }
  
  /**
   * Die Kontoauswahl kann sich das zuletzt ausgewaehlte Konto merken.
   * Damit dann aber nicht auf allen Dialogen das gleiche Konto vorausgewaehlt ist,
   * kann man hier einen individuellen Freitext-Token uebergeben, der als Key fuer
   * das Speichern des zuletzt ausgewaehlten Kontos verwendet wird. Ueberall dort,
   * wo also der gleiche Token verwendet wird, wird auch das gleiche Konto
   * vorausgewaehlt. Der Konto kann z.Bsp. "auswertungen" heissen. Wenn dieser
   * auf allen Dialogen der Auswertungen verwendet wird, wird dort dann auch ueberall
   * das gleiche Konto vorausgewaehlt sein.
   * @param s der Restore-Token.
   */
  public void setRememberSelection(String s)
  {
    if (s == null || s.length() == 0)
      return;
    
    this.token = s;

    String id = settings.getString(this.token,null);
    if (id != null && id.length() > 0)
    {
      try
      {
        Konto k = (Konto) Settings.getDBService().createObject(Konto.class,id);
        this.setPreselected(k);
      }
      catch (Exception e)
      {
        // Konto konnte nicht geladen werden. Vorauswahl loeschen
        settings.setAttribute(this.token,(String) null);
      }
    }
    
    // Listener hinzufuegen
    this.addListener(new Listener() {
      public void handleEvent(Event event)
      {
        storeSelection();
      }
    });
  }
  
  /**
   * @see de.willuhn.jameica.gui.input.SelectInput#getControl()
   */
  public Control getControl()
  {
    if (this.control != null)
      return this.control;
    
    this.control = super.getControl();
    
    Application.getMessagingFactory().registerMessageConsumer(this.mc);
    this.control.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e)
      {
        Application.getMessagingFactory().unRegisterMessageConsumer(mc);
        if (token != null)
          storeSelection();
      }
    });
    return this.control;
  }
  
  /**
   * Speichert die aktuelle Auswahl.
   */
  private void storeSelection()
  {
    try
    {
      Konto k = (Konto) getValue();
      settings.setAttribute(token,(String) (k != null ? k.getID() : null));
    }
    catch (Exception e)
    {
      // Hier lief was schief. Wir loeschen die Vorauswahl
      settings.setAttribute(token,(String) null);
    }
  }

  /**
   * Initialisiert die Liste der Konten.
   * @param filter Konto-Filter.
   * @return Liste der Konten.
   * @throws RemoteException
   */
  private static List init(KontoFilter filter) throws RemoteException
  {
    groups = KontoUtil.getGroups(); // Gruppen neu laden
    boolean haveGroups = groups.size() > 0;
    
    DBIterator it = Settings.getDBService().createList(Konto.class);
    it.setOrder("ORDER BY LOWER(kategorie), blz, kontonummer");
    List l = new ArrayList();
    
    String current = null;
    
    while (it.hasNext())
    {
      Konto k = (Konto) it.next();
      
      if (filter == null || filter.accept(k))
      {
        if (haveGroups)
        {
          String kat = StringUtils.trimToNull(k.getKategorie());
          if (kat != null) // haben wir eine Kategorie?
          {
            if (current == null || !kat.equals(current)) // Neue Kategorie?
            {
              l.add(kat);
              current = kat;
            }
          }
        }
        l.add(k);
      }
    }
    return l;
  }
  
  /**
   * @see de.willuhn.jameica.gui.input.SelectInput#getValue()
   */
  public Object getValue()
  {
    Object o = super.getValue();
    if (o instanceof String) // Kategorie
    {
      GUI.getView().setErrorText(i18n.tr("Auswahl von Konto-Gruppen wird noch nicht unterst�tzt"));
      return null;
    }
    return o;
  }

  /**
   * @see de.willuhn.jameica.gui.input.SelectInput#format(java.lang.Object)
   */
  protected String format(Object bean)
  {
    if (bean == null)
      return null;
    
    if (!(bean instanceof Konto))
      return bean.toString();
    
    try
    {
      Konto k = (Konto) bean;
      
      Konto kd = Settings.getDefaultKonto();
      boolean isDefault = (kd != null && kd.equals(k));


      boolean disabled = k.hasFlag(Konto.FLAG_DISABLED);

      StringBuffer sb = new StringBuffer();
      if (groups.size() > 0)
        sb.append("   "); // Wir haben Gruppen - also einruecken
      if (isDefault)
        sb.append("> ");
      if (disabled)
        sb.append("[");
      
      sb.append(i18n.tr("Kto. {0}",k.getKontonummer()));
      
      String blz = k.getBLZ();
      sb.append(" [");
      String bankName = HBCIUtils.getNameForBLZ(blz);
      if (bankName != null && bankName.length() > 0)
      {
        sb.append(bankName);
      }
      else
      {
        sb.append("BLZ ");
        sb.append(blz);
      }
      sb.append("] ");
      sb.append(k.getName());

      String bez = k.getBezeichnung();
      if (bez != null && bez.length() > 0)
      {
        sb.append(" - ");
        sb.append(bez);
      }
      
      if (k.getSaldoDatum() != null)
      {
        sb.append(", ");
        sb.append(i18n.tr("Saldo: {0} {1}", new String[]{HBCI.DECIMALFORMAT.format(k.getSaldo()),k.getWaehrung()}));
      }
      
      if (disabled)
        sb.append("]");
      return sb.toString();
    }
    catch (RemoteException re)
    {
      Logger.error("unable to format address",re);
      return null;
    }
  }

  /**
   * Listener, der die Auswahl des Kontos ueberwacht und den Kommentar anpasst.
   */
  private class KontoListener implements Listener
  {
    /**
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    public void handleEvent(Event event) {

      try {
        Object o = getValue();
        if (o == null || !(o instanceof Konto))
        {
          setComment("");
          return;
        }

        Konto konto = (Konto) o;
        String w = konto.getWaehrung();

        Date datum = konto.getSaldoDatum();
        if (datum != null)
          setComment(i18n.tr("Saldo: {0} {1} vom {2}", new String[]{HBCI.DECIMALFORMAT.format(konto.getSaldo()),w,HBCI.DATEFORMAT.format(datum)}));
        else
          setComment("");
      }
      catch (RemoteException er)
      {
        Logger.error("error while updating currency",er);
        GUI.getStatusBar().setErrorText(i18n.tr("Fehler bei Ermittlung der W�hrung"));
      }
    }
  }

  /**
   * Wird ueber Saldo-Aenderungen benachrichtigt.
   */
  private class SaldoMessageConsumer implements MessageConsumer
  {
    /**
     * @see de.willuhn.jameica.messaging.MessageConsumer#getExpectedMessageTypes()
     */
    public Class[] getExpectedMessageTypes()
    {
      return new Class[]{SaldoMessage.class};
    }

    /**
     * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
     */
    public void handleMessage(Message message) throws Exception
    {
      SaldoMessage msg = (SaldoMessage) message;
      GenericObject o = msg.getObject();
      if (!(o instanceof Konto))
        return;
      
      final Konto konto = (Konto) o;
      
      GUI.getDisplay().syncExec(new Runnable() {
        public void run()
        {
          // Checken, ob wir das Konto in der Liste haben. Wenn ja, aktualisieren
          // wir dessen Saldo
          List list = null;
          
          try
          {
            list = getList();
            
            if (list == null)
              return;

            for (int i=0;i<list.size();++i)
            {
              Konto k = (Konto) list.get(i);
              if (BeanUtil.equals(konto,k))
              {
                k.setSaldo(konto.getSaldo());
                break;
              }
            }
            
            // Liste neu zeichnen lassen. Das aktualisiert die Kommentare
            // und den Text in der Kombo-Box
            setValue(getValue());
            setList(list);
            if (listener != null)
              listener.handleEvent(null);
          }
          catch (NoSuchMethodError e)
          {
            // TODO "getList" hab ich erst am 15.04. eingebaut. Das catch kann hier also mal irgendwann weg
            Logger.warn(e.getMessage() + " - update your jameica installation");
          }
          catch (Exception e)
          {
            Logger.error("unable to refresh konto",e);
          }
        }
      });
    }

    /**
     * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
     */
    public boolean autoRegister()
    {
      return false;
    }
  }

}
