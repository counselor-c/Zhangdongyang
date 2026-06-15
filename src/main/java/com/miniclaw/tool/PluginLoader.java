package com.miniclaw.tool;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {
    
    public static void loadPluginsFromDir(String dirPath) {
        File pluginDir = new File(dirPath);
        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            System.out.println("[Plugin] Plugin directory '" + dirPath + "' does not exist. Creating it...");
            pluginDir.mkdirs();
            return;
        }

        File[] jarFiles = pluginDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("[Plugin] No external .jar plugins found in '" + dirPath + "'.");
            return;
        }

        for (File jarFile : jarFiles) {
            try {
                System.out.println("\n[Plugin] Discovered external jar: " + jarFile.getName());
                // 1. Create a dedicated ClassLoader for this JAR (Isolation)
                URL[] urls = { jarFile.toURI().toURL() };
                // Use Tool.class.getClassLoader() as parent to ensure they share the same Tool interface
                URLClassLoader pluginClassLoader = new URLClassLoader(urls, Tool.class.getClassLoader());

                // 2. Scan entries inside the JAR
                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                            String className = entry.getName().replace("/", ".").replace(".class", "");
                            try {
                                // 3. Load class using the plugin's ClassLoader
                                Class<?> clazz = Class.forName(className, true, pluginClassLoader);
                                
                                // 4. Check if it implements Tool interface and is instantiable
                                if (Tool.class.isAssignableFrom(clazz) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                                    Tool tool = (Tool) clazz.getDeclaredConstructor().newInstance();
                                    ToolRegistry.getInstance().register(tool);
                                    System.out.println("  -> Successfully loaded and registered: " + tool.getName() + " (from " + jarFile.getName() + ")");
                                }
                            } catch (Throwable t) {
                                // Ignore classes that cannot be loaded (e.g., missing dependencies)
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Plugin] Failed to load plugin jar: " + jarFile.getName());
                e.printStackTrace();
            }
        }
    }
}
