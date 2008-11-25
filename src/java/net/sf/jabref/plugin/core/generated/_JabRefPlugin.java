package net.sf.jabref.plugin.core.generated;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.sf.jabref.plugin.util.RuntimeExtension;
import org.java.plugin.Plugin;
import org.java.plugin.PluginLifecycleException;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Do not modify this file, as it was auto generated and will be overwritten!
 * User modifications should go in net.sf.jabref.plugin.core.JabRefPlugin.
 */
public abstract class _JabRefPlugin extends Plugin {

    public static String getId(){
        return "net.sf.jabref.core";
    }

	static Log log = LogFactory.getLog(_JabRefPlugin.class);

	public List<ExportFormatTemplateExtension> getExportFormatTemplateExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "ExportFormatTemplate");
        List<ExportFormatTemplateExtension> result = new ArrayList<ExportFormatTemplateExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new ExportFormatTemplateExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class ExportFormatTemplateExtension extends RuntimeExtension {
        public ExportFormatTemplateExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              public String getDisplayName(){
            return getStringParameter("displayName");
        }
  
  	     
              public String getConsoleName(){
            return getStringParameter("consoleName");
        }
  
  	     
              public String getLayoutFilename(){
            return getStringParameter("layoutFilename");
        }
  
  	     
      		public URL getDirAsUrl(){
		    return getResourceParameter("dir");
		}
		
		public URL getDirAsUrl(String relativePath){
		    return getResourceParameter("dir", relativePath);
		}
  
  	     
              public String getExtension(){
            return getStringParameter("extension");
        }
  
  	     
              public String getEncoding(){
            return getStringParameter("encoding");
        }
  
      }

	public List<ExportFormatExtension> getExportFormatExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "ExportFormat");
        List<ExportFormatExtension> result = new ArrayList<ExportFormatExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new ExportFormatExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class ExportFormatExtension extends RuntimeExtension {
        public ExportFormatExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              /**
         * @return A singleton instance of the class parameter or null if the class could not be found!
         */
        public net.sf.jabref.export.IExportFormat getExportFormat(){
          return (net.sf.jabref.export.IExportFormat)getClassParameter("exportFormat");
        }
  
  	     
              public String getDisplayName(){
            return getStringParameter("displayName");
        }
  
  	     
              public String getConsoleName(){
            return getStringParameter("consoleName");
        }
  
  	     
              public String getExtension(){
            return getStringParameter("extension");
        }
  
      }

	public List<SidePanePluginExtension> getSidePanePluginExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "SidePanePlugin");
        List<SidePanePluginExtension> result = new ArrayList<SidePanePluginExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new SidePanePluginExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class SidePanePluginExtension extends RuntimeExtension {
        public SidePanePluginExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              /**
         * @return A singleton instance of the class parameter or null if the class could not be found!
         */
        public net.sf.jabref.plugin.SidePanePlugin getSidePanePlugin(){
          return (net.sf.jabref.plugin.SidePanePlugin)getClassParameter("sidePanePlugin");
        }
  
  	     
              public String getName(){
            return getStringParameter("name");
        }
  
  	     
              public String getDescription(){
            return getStringParameter("description");
        }
  
      }

	public List<EntryFetcherExtension> getEntryFetcherExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "EntryFetcher");
        List<EntryFetcherExtension> result = new ArrayList<EntryFetcherExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new EntryFetcherExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class EntryFetcherExtension extends RuntimeExtension {
        public EntryFetcherExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              /**
         * @return A singleton instance of the class parameter or null if the class could not be found!
         */
        public net.sf.jabref.imports.EntryFetcher getEntryFetcher(){
          return (net.sf.jabref.imports.EntryFetcher)getClassParameter("entryFetcher");
        }
  
  	     
              public String getName(){
            return getStringParameter("name");
        }
  
  	     
              public String getDescription(){
            return getStringParameter("description");
        }
  
      }

	public List<ExportFormatProviderExtension> getExportFormatProviderExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "ExportFormatProvider");
        List<ExportFormatProviderExtension> result = new ArrayList<ExportFormatProviderExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new ExportFormatProviderExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class ExportFormatProviderExtension extends RuntimeExtension {
        public ExportFormatProviderExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              /**
         * @return A singleton instance of the class parameter or null if the class could not be found!
         */
        public net.sf.jabref.export.IExportFormatProvider getFormatProvider(){
          return (net.sf.jabref.export.IExportFormatProvider)getClassParameter("formatProvider");
        }
  
  	     
              public String getName(){
            return getStringParameter("name");
        }
  
  	     
              public String getDescription(){
            return getStringParameter("description");
        }
  
      }

	public List<PushToApplicationExtension> getPushToApplicationExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "PushToApplication");
        List<PushToApplicationExtension> result = new ArrayList<PushToApplicationExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new PushToApplicationExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class PushToApplicationExtension extends RuntimeExtension {
        public PushToApplicationExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              /**
         * @return A singleton instance of the class parameter or null if the class could not be found!
         */
        public net.sf.jabref.external.PushToApplication getPushToApp(){
          return (net.sf.jabref.external.PushToApplication)getClassParameter("pushToApp");
        }
  
  	     
              public String getName(){
            return getStringParameter("name");
        }
  
  	     
              public String getDescription(){
            return getStringParameter("description");
        }
  
      }

	public List<LayoutFormatterExtension> getLayoutFormatterExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "LayoutFormatter");
        List<LayoutFormatterExtension> result = new ArrayList<LayoutFormatterExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new LayoutFormatterExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class LayoutFormatterExtension extends RuntimeExtension {
        public LayoutFormatterExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              /**
         * @return A singleton instance of the class parameter or null if the class could not be found!
         */
        public net.sf.jabref.export.layout.LayoutFormatter getLayoutFormatter(){
          return (net.sf.jabref.export.layout.LayoutFormatter)getClassParameter("layoutFormatter");
        }
  
  	     
              public String getDescription(){
            return getStringParameter("description");
        }
  
  	     
              public String getName(){
            return getStringParameter("name");
        }
  
      }

	public List<ImportFormatExtension> getImportFormatExtensions(){
        ExtensionPoint extPoint = getManager().getRegistry().getExtensionPoint(getId(), "ImportFormat");
        List<ImportFormatExtension> result = new ArrayList<ImportFormatExtension>();
        for (Extension ext : extPoint.getConnectedExtensions()) {
			try {
				result.add(new ImportFormatExtension(getManager().getPlugin(
						ext.getDeclaringPluginDescriptor().getId()), ext));
			} catch (PluginLifecycleException e) {
				log.error("Failed to activate plug-in " + ext.getDeclaringPluginDescriptor().getId(), e);
			}
		}
        return result;
    }

    public static class ImportFormatExtension extends RuntimeExtension {
        public ImportFormatExtension(Plugin declaringPlugin, Extension wrapped){
            super(declaringPlugin, wrapped);
        }
                
	     
              /**
         * @return A singleton instance of the class parameter or null if the class could not be found!
         */
        public net.sf.jabref.imports.ImportFormat getImportFormat(){
          return (net.sf.jabref.imports.ImportFormat)getClassParameter("importFormat");
        }
  
  	     
              public String getName(){
            return getStringParameter("name");
        }
  
  	     
              public String getDescription(){
            return getStringParameter("description");
        }
  
      }

}
