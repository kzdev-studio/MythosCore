package pl.mythoscore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mixin(targets = "org.bukkit.plugin.java.PluginClassLoader", remap = false)
public abstract class PluginClassLoaderMixin extends URLClassLoader {

    protected PluginClassLoaderMixin(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Inject(method = "findClass", at = @At("HEAD"), cancellable = true)
    protected void onFindClass(String name, CallbackInfoReturnable<Class<?>> cir) {
        try {
            cir.setReturnValue(super.findClass(name));
        } catch (ClassNotFoundException e) {
            try {
                URL url = getURLs()[0];
                File file = new File(url.toURI());
                try (JarFile jar = new JarFile(file)) {
                    JarEntry entry = jar.getJarEntry(name.replace('.', '/') + ".class");
                    if (entry != null) {
                        try (InputStream is = jar.getInputStream(entry)) {
                            byte[] bytes = is.readAllBytes();
                            cir.setReturnValue(defineClass(name, bytes, 0, bytes.length));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}