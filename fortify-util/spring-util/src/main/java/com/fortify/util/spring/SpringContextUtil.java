package com.fortify.util.spring;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

public final class SpringContextUtil {
	private static final Log LOG = LogFactory.getLog(SpringContextUtil.class);
	private static final Map<Class<?>, Class<? extends PropertyEditor>> PROPERTY_EDITORS = getPropertyEditors();
	
	private SpringContextUtil() {}
	
	/**
	 * Automatically load all {@link PropertyEditorWithTargetClass} implementations
	 * (annotated with {@link Component}) from 
	 * com.fortify.util.spring.propertyeditor (sub-)packages. 
	 * @return
	 */
	private static final Map<Class<?>, Class<? extends PropertyEditor>> getPropertyEditors() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext("com.fortify.util.spring.propertyeditor");
		try {
			Map<Class<?>, Class<? extends PropertyEditor>> result = new HashMap<Class<?>, Class<? extends PropertyEditor>>();
			Map<String, PropertyEditorWithTargetClass> beans = ctx.getBeansOfType(PropertyEditorWithTargetClass.class);
			for ( Map.Entry<String, PropertyEditorWithTargetClass> entry : beans.entrySet() ) {
				Class<?> targetClass = entry.getValue().getTargetClass();
				Class<? extends PropertyEditor> propertyEditorClass = entry.getValue().getClass();
				result.put(targetClass, propertyEditorClass);
			}
			LOG.info("Loaded PropertyEditors for classes: "+result);
			return result;
		} finally {
			ctx.close();
		}
	}
	
	/**
	 * Get a basic Spring {@link GenericApplicationContext} with additional 
	 * custom editors registered. The additional custom editors are automatically
	 * loaded from com.fortify.util.spring.propertyeditor (sub-)packages
	 * if they have the {@link Component} annotation.
	 * @return Basic Spring ApplicationContext with custom editors registered.
	 */
	public static final GenericApplicationContext getBaseContext() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.setClassLoader(SpringContextUtil.class.getClassLoader());
		
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		cec.setCustomEditors(PROPERTY_EDITORS);
		context.addBeanFactoryPostProcessor(cec);
		return context;
	}
	
	/**
	 * Add XML-based configuration from the given resource names to the given context.
	 * See {@link DefaultResourceLoader} for a description on how to format the
	 * resource names.
	 * After all resources have been added to the context, the context may need to
	 * be refreshed in order to process all bean definitions.
	 * @param context
	 * @param resourceNames
	 */
	public static final void addConfigurationFromXmlResources(BeanDefinitionRegistry context, boolean errorOnMissingResource, String... resourceNames) {
		ResourceLoader resourceLoader = new DefaultResourceLoader(); 
		for ( String resourceName : resourceNames ) {
			Resource resource = resourceLoader.getResource(resourceName);
			addConfigurationFromXmlResource(context, resource, errorOnMissingResource);
		}
	}
	
	/**
	 * Add XML-based configuration from the given file names to the given context.
	 * After all files have been added to the context, the context may need to
	 * be refreshed in order to process all bean definitions.
	 * @param context
	 * @param fileNames
	 */
	public static final void addConfigurationFromXmlFiles(BeanDefinitionRegistry context, boolean errorOnMissingResource, String... fileNames) {
		for ( String fileName : fileNames ) {
			Resource resource = new FileSystemResource(fileName);
			addConfigurationFromXmlResource(context, resource, errorOnMissingResource);
		}
	}

	public static void addConfigurationFromXmlResource(BeanDefinitionRegistry context, Resource resource, boolean errorOnMissingResource) {
		BeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
		if ( !resource.exists() ) {
			if ( errorOnMissingResource ) {
				throw new RuntimeException("Resource "+resource.getFilename()+" does not exist");
			} else {
				LOG.info("Resource "+resource.getFilename()+" does not exist; no bean definitions will be added");
			}
		} else {
			LOG.info("Loading bean definitions from resource "+resource.getFilename());
			reader.loadBeanDefinitions(resource);
		}
	}
	
	/**
	 * <p>Load a Spring {@link GenericApplicationContext} from the given resource names.
	 * See {@link DefaultResourceLoader} for a description on how to format the
	 * resource names.</p>
	 * 
	 * <p>This method will refresh the context after loading all resources, this
	 * means that no more resources can be added to the returned context.</p>
	 * 
	 * @param fileNames
	 * @return
	 */
	public static final GenericApplicationContext loadApplicationContextFromResources(boolean errorOnMissingResource, String... resourceNames) {
		GenericApplicationContext result = getBaseContext();
		addConfigurationFromXmlResources(result, errorOnMissingResource, resourceNames);
		result.refresh();
		return result;
	}
	
	/**
	 * <p>Load a Spring {@link GenericApplicationContext} from the given file names.</p>
	 * 
	 * <p>This method will refresh the context after loading all resources, this
	 * means that no more resources can be added to the returned context.</p>
	 * 
	 * @param fileNames
	 * @return
	 */
	public static final GenericApplicationContext loadApplicationContextFromFiles(boolean errorOnMissingResource, String... fileNames) {
		GenericApplicationContext result = getBaseContext();
		addConfigurationFromXmlFiles(result, errorOnMissingResource, fileNames);
		result.refresh();
		return result;
	}
	
	public static interface PropertyEditorWithTargetClass extends PropertyEditor {
		public Class<?> getTargetClass();
	}
}
