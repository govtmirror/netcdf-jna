package edu.unidata.ucar.netcdf.jna;

import com.sun.jna.Native.DeleteNativeLibrary;
import com.sun.jna.Platform;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.unidata.ucar.netcdf.jna.NC.*;
import static java.nio.ByteOrder.nativeOrder;

/**
 *
 * @author tkunicki
 */
public class NCUtil {

    public static void status(int status) {
        if(status != NC_NOERR) {
            throw new RuntimeException(nc_strerror(status));
        }
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, String[] values) {
        int ncStatus;
        IntByReference iRef = new IntByReference();

        int count = values.length;
        int size = 0;
        int[] offset = new int[values.length];
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; ++index) {
            String value = values[index];
            offset[index] = size;
            size += value.length();
            builder.append(value);
        }

        ncStatus = nc_def_compound(ncId, new NativeLong(size), cname, iRef);
        status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < count; ++index) {
            ncStatus = nc_insert_array_compound(ncId, typeid, fields[index], new NativeLong(offset[index]), NC_CHAR, values[index].length());
            status(ncStatus);
        }

        byte[] attBytes = null;
        try {
            attBytes = builder.toString().getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(NCUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        ByteBuffer attBuffer = ByteBuffer.wrap(attBytes);
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), attBuffer);
        status(ncStatus);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, byte[] values) {
        XType xtype = XType.findXType(values);
		long size = xtype.getSizeBytes(values.length);
        Buffer buffer = allocateByteBuffer(size).put(values).rewind();
        generateCompoundAttributes(ncId, varId, cname, aname, fields, xtype, buffer);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, short[] values) {
        XType xtype = XType.findXType(values);
		long size = xtype.getSizeBytes(values.length);
        Buffer buffer = allocateByteBuffer(size).asShortBuffer().put(values).rewind();
        generateCompoundAttributes(ncId, varId, cname, aname, fields, xtype, buffer);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, int[] values) {
        XType xtype = XType.findXType(values);
		long size = xtype.getSizeBytes(values.length);
        Buffer buffer = allocateByteBuffer(size).asIntBuffer().put(values).rewind();
        generateCompoundAttributes(ncId, varId, cname, aname, fields, xtype, buffer);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, long[] values) {
        XType xtype = XType.findXType(values);
		long size = xtype.getSizeBytes(values.length);
        Buffer buffer = allocateByteBuffer(size).asLongBuffer().put(values).rewind();
        generateCompoundAttributes(ncId, varId, cname, aname, fields, xtype, buffer);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, float[] values) {
        XType xtype = XType.findXType(values);
		long size = xtype.getSizeBytes(values.length);
        Buffer buffer = allocateByteBuffer(size).asFloatBuffer().put(values).rewind();
        generateCompoundAttributes(ncId, varId, cname, aname, fields, xtype, buffer);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, double[] values) {
		XType xtype = XType.findXType(values);
		long size = xtype.getSizeBytes(values.length);
        DoubleBuffer buffer = allocateByteBuffer(size).asDoubleBuffer();
		buffer.put(values).rewind();
        generateCompoundAttributes(ncId, varId, cname, aname, fields, xtype, buffer);
    }

	// if visibility increased, one must check that buffer is direct and order is native.
	private static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, XType xtype, Buffer buffer) {
		int ncStatus;
        IntByReference iRef = new IntByReference();
		int count = buffer.limit() - buffer.position();
		long sizeBytes =  xtype.getSizeBytes(count);
        ncStatus = nc_def_compound(ncId, new NativeLong(sizeBytes), cname, iRef); status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < count; ++index) {
            ncStatus = nc_insert_compound(ncId, typeid, fields[index], new NativeLong(index * xtype.sizeBytes), xtype.code);
            status(ncStatus);
        }
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), buffer); status(ncStatus);
    }

	private static ByteBuffer allocateByteBuffer(long size) {
		if (size > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("can't allocate " + size + " bytes with this method, use JNA Memory instead");
		}
		return ByteBuffer.allocateDirect((int)size).order(nativeOrder());
	}
	
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
                
        // Add an ugly hack for OpenJDK (soylatte) - JNI libs use the usual
        // .dylib extension 
        if (url == null && Platform.isMac()
            && resourceName.endsWith(".dylib")) {
            resourceName = resourceName.substring(0, resourceName.lastIndexOf(".dylib")) + ".jnilib";
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

	public enum XType {
		NC_NAT(0, -1),		/* NAT = 'Not A Type' (c.f. NaN) */
		NC_BYTE(1, 1),		/* signed 1 byte integer */
		NC_CHAR(2, 1),		/* ISO/ASCII character */
		NC_SHORT(3, 2),		/* signed 2 byte integer */
		NC_INT(4, 4),		/* signed 4 byte integer */
		NC_LONG(4, 4),		/* deprecated, but required for backward compatibility. */
		NC_FLOAT(5, 4),		/* single precision floating point number */
		NC_DOUBLE(6, 8),	/* double precision floating point number */
		NC_UBYTE(7, 1),		/* unsigned 1 byte int */
		NC_USHORT(8, 2),	/* unsigned 2-byte int */
		NC_UINT(9, 4),		/* unsigned 4-byte int */
		NC_INT64(10, 8),	/* signed 8-byte int */
		NC_UINT64(11, 8),	/* unsigned 8-byte int */
		NC_STRING(12, -1);  /* string */

		final int code;
		final int sizeBytes;

		XType (int xtype, int sizeBytes) {
			this.code = xtype;
			this.sizeBytes = sizeBytes;
		}

		public int getSizeBytes() {
			return sizeBytes;
		}

		public long getSizeBytes(long count) {
			if (count < 0) {
				throw new IllegalArgumentException("count parameter < 0");
			}
			if (sizeBytes < 1) {
				throw new IllegalArgumentException("can't calculate size for xtype " + this);
			}
			// i forget if this is required, but JVM will compile it out if not...
			return (long)count * (long)sizeBytes;
		}

		public int getCode() {
			return code;
		}

		public static XType findXType(int code) {
			if (code < 0 || code > values().length) {
				throw new IllegalArgumentException("Must provide a valid xtype");
			} else {
				for (XType xtype : values()) {
					if (code == xtype.code) {
						return xtype;
					}
				}
			}
			return NC_NAT;

		}
		
		public static XType findXType(Object o) {
			Class clazz = o.getClass();
			if (clazz.isArray()) {
				clazz = clazz.getComponentType();
			}

			if (clazz == byte.class) {
				return NC_BYTE;
			}
			if (clazz == short.class) {
				return NC_SHORT;
			}
			if (clazz == int.class) {
				return NC_INT;
			}
			if (clazz == long.class) {
				return NC_INT64;
			}
			if (clazz == float.class) {
				return NC_FLOAT;
			}
			if (clazz == double.class) {
				return NC_DOUBLE;
			}

			return NC_NAT;
		}
	}

}
