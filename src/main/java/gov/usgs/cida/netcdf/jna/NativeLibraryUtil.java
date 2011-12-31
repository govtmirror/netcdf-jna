package gov.usgs.cida.netcdf.jna;

import com.sun.jna.Native.DeleteNativeLibrary;
import com.sun.jna.Platform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 *
 * @author tkunicki
 */
public class NativeLibraryUtil {
    /*
	 * Copied from jna code
     * Attempts to load the native library resource from the filesystem,
     * extracting the JNA stub library from jna.jar if not already available.
     */
    public static String loadNativeLibraryFromJar(String name_of_lib) {
        //String libname = System.mapLibraryName("jnidispatch");
		String libname = System.mapLibraryName(name_of_lib);
        String arch = System.getProperty("os.arch");
        String name = System.getProperty("os.name");
        String resourceName = getNativeLibraryResourcePath(Platform.getOSType(), arch, name) + "/" + libname;
        URL url = NCUtil.class.getResource(resourceName);
                
        // Some confusoin with correct extension on Mac platform. 
        if (url == null && Platform.isMac()) {
            if (resourceName.endsWith(".dylib")) {
                resourceName = resourceName.substring(0, resourceName.lastIndexOf(".dylib")) + ".jnilib";
            } else if (resourceName.endsWith(".jnilib")) {
                resourceName = resourceName.substring(0, resourceName.lastIndexOf(".jnilib")) + ".dylib";
            }
            url = NCUtil.class.getResource(resourceName);
        }
        if (url == null) {
            throw new UnsatisfiedLinkError(libname + " (" + resourceName 
                                           + ") not found in resource path");
        }
    
        File libdir = null;
        File lib = null;
        if (url.getProtocol().toLowerCase().equals("file")) {
            try {
                lib = new File(new URI(url.toString()));
                libdir = lib.getParentFile();
            }
            catch(URISyntaxException e) {
                lib = new File(url.getPath());
            }
            if (!lib.exists()) {
                throw new Error("File URL " + url + " could not be properly decoded");
            }
        }
        else {
            InputStream is = NCUtil.class.getResourceAsStream(resourceName);
            if (is == null) {
                throw new Error("Can't obtain " + libname + " InputStream");
            }
            
            FileOutputStream fos = null;
            try {
                // Suffix is required on windows, or library fails to load
                // Let Java pick the suffix, except on windows, to avoid
                // problems with Web Start.
                //lib = File.createTempFile("jna", Platform.isWindows()?".dll":null);
                //libdir = File.createTempFile("jna", null);
                libdir = new File(System.getProperty("java.io.tmpdir") + File.separator + "jna");
                libdir.mkdir();
                libdir.deleteOnExit();
                
                lib = new File(libdir, libname);
                lib.deleteOnExit();
                
                ClassLoader cl = NCUtil.class.getClassLoader();
                if (Platform.deleteNativeLibraryAfterVMExit()
                    && (cl == null
                        || cl.equals(ClassLoader.getSystemClassLoader()))) {
                    Runtime.getRuntime().addShutdownHook(new DeleteNativeLibrary(lib));
                }
                fos = new FileOutputStream(lib);
                int count;
                byte[] buf = new byte[1024];
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, count);
                }
            }
            catch(IOException e) {
                throw new Error("Failed to create temporary file for jnidispatch library: " + e);
            }
            finally {
                try { is.close(); } catch(IOException e) { }
                if (fos != null) {
                    try { fos.close(); } catch(IOException e) { }
                }
            }
        }
        System.load(lib.getAbsolutePath());
        return libdir.getAbsolutePath();
    }
	
	static String getNativeLibraryResourcePath(int osType, String arch, String name) {
        String osPrefix;
        arch = arch.toLowerCase();
        switch(osType) {
        case Platform.WINDOWS:
            if ("i386".equals(arch))
                arch = "x86";
            osPrefix = "win32-" + arch;
            break;
        case Platform.MAC:
            osPrefix = "darwin";
            break;
        case Platform.LINUX:
            if ("x86".equals(arch)) {
                arch = "i386";
            }
            else if ("x86_64".equals(arch)) {
                arch = "amd64";
            }
            osPrefix = "linux-" + arch;
            break;
        case Platform.SOLARIS:
            osPrefix = "sunos-" + arch;
            break;
        default:
            osPrefix = name.toLowerCase();
            if ("x86".equals(arch)) {
                arch = "i386";
            }
            if ("x86_64".equals(arch)) {
                arch = "amd64";
            }
            if ("powerpc".equals(arch)) {
                arch = "ppc";
            }
            int space = osPrefix.indexOf(" ");
            if (space != -1) {
                osPrefix = osPrefix.substring(0, space);
            }
            osPrefix += "-" + arch;
            break;
        }
        return "/com/sun/jna/" + osPrefix;
    }
    
}
