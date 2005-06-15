/**********************************************************************
 * $Source: /cvsroot/jameica/jameica/src/de/willuhn/jameica/gui/internal/parts/ServiceList.java,v $
 * $Revision: 1.3 $
 * $Date: 2005/06/15 17:51:31 $
 * $Author: web0 $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.gui.internal.parts;

import java.rmi.RemoteException;

import de.willuhn.datasource.GenericIterator;
import de.willuhn.datasource.GenericObject;
import de.willuhn.datasource.Service;
import de.willuhn.datasource.pseudo.PseudoIterator;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.dialogs.YesNoDialog;
import de.willuhn.jameica.gui.internal.dialogs.ServiceBindingDialog;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.plugin.ServiceDescriptor;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.jameica.system.ServiceSettings;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * Eine vorkonfektionierte Liste der Services eines Plugins.
 */
public class ServiceList extends TablePart
{

  /**
   * ct.
   * @param plugin
   */
  public ServiceList(final AbstractPlugin plugin)
  {
    super(init(plugin),new CustomAction());

    final I18N i18n = Application.getI18n();

    ContextMenu menu = new ContextMenu();
    menu.addItem(new CheckedContextMenuItem(i18n.tr("mit Server verbinden..."),new Action()
    {
      public void handleAction(Object context) throws ApplicationException
      {
        ServiceObject so = (ServiceObject) context;
        if (so == null)
          return;
        
        String fullName = so.plugin.getClass().getName() + "." + so.serviceName;
        ServiceBindingDialog d = new ServiceBindingDialog(fullName, ServiceBindingDialog.POSITION_CENTER);
        try
        {
          String s = (String) d.open();
          if (s == null || s.length() == 0)
            return;
          String[] host = s.split(":");
          ServiceSettings.setLookup(fullName,host[0],Integer.parseInt(host[1]));
          GUI.startView(GUI.getCurrentView().getClass().getName(),plugin); // Tabelle aktualisieren
          GUI.getStatusBar().setSuccessText(Application.getI18n().tr("Server-Einstellungen gespeichert"));
        }
        catch (OperationCanceledException oce)
        {
          Logger.info("operation cancelled");
        }
        catch (Exception e)
        {
          Logger.error("error while entering service bindings",e);
          GUI.getStatusBar().setErrorText(Application.getI18n().tr("Fehler beim ‹bernehmen der Server-Einstellungen"));
        }
        
      }
    })
    {
      public boolean isEnabledFor(Object o)
      {
        try
        {
          // Die Option bieten wir nur im Client-Mode
          // an weil in den beiden anderen Modi immer lokale Services verwendet werden
          return o != null && Application.inClientMode();
        }
        catch (Exception e)
        {
          Logger.error("Error while checking service binding",e);
        }
        return false;
      }
    });

    menu.addItem(new CheckedContextMenuItem(i18n.tr("Service stoppen..."),new Action()
    {
      public void handleAction(final Object context) throws ApplicationException
      {
        YesNoDialog d = new YesNoDialog(YesNoDialog.POSITION_CENTER);
        d.setTitle(i18n.tr("Service stoppen"));
        d.setText(i18n.tr("Sind Sie sicher, dass Sie den Service stoppen wollen?"));
        boolean doIt = false;
        try
        {
          doIt = ((Boolean) d.open()).booleanValue();
        }
        catch (Exception e)
        {
          Logger.error("error while stopping service",e);
          GUI.getStatusBar().setErrorText(i18n.tr("Fehler beim Stoppen des Service"));
        }
        if (!doIt) return;

        GUI.startSync(new Runnable()
        {
          public void run()
          {
            try
            {
              GUI.getStatusBar().setSuccessText(i18n.tr("Service wird gestoppt"));
              GUI.getStatusBar().startProgress();
              ServiceObject so = (ServiceObject) context;
              so.service.stop(true);
              GUI.startView(GUI.getCurrentView().getClass().getName(),plugin);
              GUI.getStatusBar().setSuccessText(i18n.tr("Service gestoppt"));
            }
            catch (Exception e)
            {
              GUI.getStatusBar().setErrorText(i18n.tr("Fehler beim Stoppen des Service"));
              Logger.error("Error while stopping service",e);
            }
            finally
            {
              GUI.getStatusBar().stopProgress();
            }
          }
        });
      }
    })
    {
      public boolean isEnabledFor(Object o)
      {
        try
        {
          ServiceObject so = (ServiceObject) o;
          if (o == null)
            return false;
          // Die Option bieten wir nicht im Client-Mode
          // an weil wir nicht wollen, dass der User Serverdienste stoppt
          return (!Application.inClientMode()) && so.service.isStarted();
        }
        catch (Exception e)
        {
          Logger.error("Error while checking service status",e);
        }
        return false;
      }
    });
  
    menu.addItem(new CheckedContextMenuItem(i18n.tr("Service starten..."),new Action()
    {
      public void handleAction(final Object context) throws ApplicationException
      {
        GUI.startSync(new Runnable()
        {
          public void run()
          {
            try
            {
              GUI.getStatusBar().setSuccessText(i18n.tr("Service wird gestartet"));
              GUI.getStatusBar().startProgress();
              ServiceObject so = (ServiceObject) context;
              so.service.start();
              GUI.startView(GUI.getCurrentView().getClass().getName(),plugin);
              GUI.getStatusBar().setSuccessText(i18n.tr("Service gestartet"));
            }
            catch (Exception e)
            {
              GUI.getStatusBar().setErrorText(i18n.tr("Fehler beim Starten des Service"));
              Logger.error("Error while starting service",e);
            }
            finally
            {
              GUI.getStatusBar().stopProgress();
            }
          }
        });
      }
    })
    {
      public boolean isEnabledFor(Object o)
      {
        try
        {
          ServiceObject so = (ServiceObject) o;
          if (o == null)
            return false;
          // Die Option bieten wir nicht im Client-Mode
          // an weil wir nicht wollen, dass der User Serverdienste startet
          return (!Application.inClientMode()) && !so.service.isStarted();
        }
        catch (Exception e)
        {
          Logger.error("Error while checking service status",e);
        }
        return false;
      }
    });
  
    setContextMenu(menu);
    addColumn(i18n.tr("Name"),"name");
    addColumn(i18n.tr("Beschreibung"),"description");
    addColumn(i18n.tr("Status"),"status");
    if (Application.inClientMode())
      addColumn(i18n.tr("Verbunden mit Server"),"binding");
    
  }

  private static class CustomAction implements Action
  {
    /**
     * @see de.willuhn.jameica.gui.Action#handleAction(java.lang.Object)
     */
    public void handleAction(Object context) throws ApplicationException
    {
    }
  }

  /**
   * Initialisiert die Liste der Services.
   * @param plugin das Plugin.
   * @return Liste der Services.
   */
  private static GenericIterator init(AbstractPlugin plugin)
  {
    ServiceDescriptor[] descriptors = plugin.getManifest().getServices();
    ServiceObject[] so = new ServiceObject[descriptors.length];
    for (int i=0;i<descriptors.length;++i)
    {
      so[i] = new ServiceObject(plugin,descriptors[i].getName());
    }
    try
    {
      return PseudoIterator.fromArray(so);
    }
    catch (RemoteException e)
    {
      Logger.error("error while loading service list",e);
      return null;
    }
  }

  
  /**
  * Ein Hilfs-Objekt, um die Services eines Plugins anzuzeigen.
  */
  private static class ServiceObject implements GenericObject
  {
  
    private AbstractPlugin plugin;
    private String serviceName;
    private Service service;
    
    /**
     * @param p
     * @param service
     */
    public ServiceObject(AbstractPlugin p, String service)
    {
      this.plugin  = p;
      this.serviceName = service;
      try
      {
        this.service = Application.getServiceFactory().lookup(this.plugin.getClass(),this.serviceName);
      }
      catch (ApplicationException ae)
      {
        GUI.getStatusBar().setErrorText(Application.getI18n().tr(ae.getMessage()));
      }
      catch (Exception e)
      {
        Logger.error("error while loading service " + serviceName,e);
      }
    }
  
    /**
     * @see de.willuhn.datasource.GenericObject#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name) throws RemoteException
    {
      if ("status".equals(name))
      {
        if (service == null)
          return Application.getI18n().tr("unbekannt");

        try
        {
          return service.isStarted() ? Application.getI18n().tr("gestartet") : Application.getI18n().tr("nicht gestartet");
        }
        catch (Exception e)
        {
          Logger.error("error while checking service status",e);
          return Application.getI18n().tr("unbekannt");
        }
      }
      if ("description".equals(name))
      {
        if (service == null)
          return "";
        try
        {
          return service.getName();
        }
        catch (Exception e)
        {
          Logger.error("error while getting service name",e);
          return "";
        }
      }
      if ("binding".equals(name))
      {
        String fullname = this.plugin.getClass().getName() + "." + this.serviceName;
        String host = ServiceSettings.getLookupHost(fullname);
        if (host == null || host.length() == 0)
          return Application.getI18n().tr("Warnung: Kein Server definiert");
        return host + ":" + ServiceSettings.getLookupPort(fullname);
      }
      return serviceName;
    }
  
    /**
     * @see de.willuhn.datasource.GenericObject#getID()
     */
    public String getID() throws RemoteException
    {
      return plugin.getClass() + "." + serviceName;
    }
  
    /**
     * @see de.willuhn.datasource.GenericObject#getPrimaryAttribute()
     */
    public String getPrimaryAttribute() throws RemoteException
    {
      return "name";
    }
  
    /**
     * @see de.willuhn.datasource.GenericObject#equals(de.willuhn.datasource.GenericObject)
     */
    public boolean equals(GenericObject other) throws RemoteException
    {
     if (other == null)
       return false;
     return other.getID().equals(getID());
    }
  
    /**
     * @see de.willuhn.datasource.GenericObject#getAttributeNames()
     */
    public String[] getAttributeNames() throws RemoteException
    {
      return new String[] {"status","description","name","binding"};
    }
  }
}


/*********************************************************************
 * $Log: ServiceList.java,v $
 * Revision 1.3  2005/06/15 17:51:31  web0
 * @N Code zum Konfigurieren der Service-Bindings
 *
 * Revision 1.2  2005/06/15 16:10:57  web0
 * @B javadoc fixes
 *
 * Revision 1.1  2005/06/14 23:15:30  web0
 * @N added settings for plugins/services
 *
 **********************************************************************/