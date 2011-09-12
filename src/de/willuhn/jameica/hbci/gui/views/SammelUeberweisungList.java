/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus/src/de/willuhn/jameica/hbci/gui/views/SammelUeberweisungList.java,v $
 * $Revision: 1.8 $
 * $Date: 2011/09/12 15:28:00 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.jameica.hbci.gui.views;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.internal.parts.PanelButtonPrint;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.gui.action.SammelUeberweisungImport;
import de.willuhn.jameica.hbci.gui.action.SammelUeberweisungNew;
import de.willuhn.jameica.hbci.gui.controller.SammelUeberweisungControl;
import de.willuhn.jameica.hbci.io.print.PrintSupportSammelUeberweisung;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

/**
 * Zeigt eine Liste mit den vorhandenen Sammel-Lastschriften an.
 */
public class SammelUeberweisungList extends AbstractView
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getI18N();

  /**
   * @see de.willuhn.jameica.gui.AbstractView#bind()
   */
  public void bind() throws Exception
  {
    SammelUeberweisungControl control = new SammelUeberweisungControl(this);
    final de.willuhn.jameica.hbci.gui.parts.SammelUeberweisungList table = control.getListe();
    final PanelButtonPrint print = new PanelButtonPrint(new PrintSupportSammelUeberweisung(table));
    table.addSelectionListener(new Listener() {
      public void handleEvent(Event event)
      {
        print.setEnabled(table.getSelection() != null);
      }
    });

    GUI.getView().setTitle(i18n.tr("Vorhandene Sammel-�berweisungen"));
    GUI.getView().addPanelButton(print);
		
    table.paint(getParent());
    print.setEnabled(table.getSelection() != null); // einmal initial ausloesen

		ButtonArea buttons = new ButtonArea();
    buttons.addButton(i18n.tr("Importieren..."),new SammelUeberweisungImport(),null,false,"document-open.png");
    buttons.addButton(i18n.tr("Neue Sammel-�berweisung"),new SammelUeberweisungNew(),null,true,"text-x-generic.png");
    buttons.paint(getParent());
  }
}


/**********************************************************************
 * $Log: SammelUeberweisungList.java,v $
 * Revision 1.8  2011/09/12 15:28:00  willuhn
 * @N Enabled-State live uebernehmen - nicht erst beim Mouse-Over
 *
 * Revision 1.7  2011-04-11 16:48:33  willuhn
 * @N Drucken von Sammel- und Dauerauftraegen
 *
 * Revision 1.6  2011-04-08 15:19:13  willuhn
 * @R Alle Zurueck-Buttons entfernt - es gibt jetzt einen globalen Zurueck-Button oben rechts
 * @C Code-Cleanup
 *
 **********************************************************************/