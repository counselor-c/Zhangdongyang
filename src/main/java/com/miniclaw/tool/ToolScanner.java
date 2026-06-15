package com.miniclaw.tool;

import org.reflections.Reflections;
import java.util.Set;

public class ToolScanner {
    public static void scanAndRegister(String packageName) {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends Tool>> toolClasses = reflections.getSubTypesOf(Tool.class);
        
        for (Class<? extends Tool> clazz : toolClasses) {
            try {
                if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    Tool tool = clazz.getDeclaredConstructor().newInstance();
                    ToolRegistry.getInstance().register(tool);
                }
            } catch (Exception e) {
                System.err.println("Failed to register tool: " + clazz.getName());
                e.printStackTrace();
            }
        }
    }
}
