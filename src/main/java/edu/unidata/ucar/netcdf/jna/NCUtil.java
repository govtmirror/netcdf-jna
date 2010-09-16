/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unidata.ucar.netcdf.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.unidata.ucar.netcdf.jna.NC.*;

/**
 *
 * @author tkunicki
 */
public class NCUtil {

    public static void status(int status) {
        if(status != NC_NOERR) {
            System.out.println(nc_strerror(status));
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
        int ncStatus;
        IntByReference iRef = new IntByReference();
        int size = values.length * 1;
        ncStatus = nc_def_compound(ncId, new NativeLong(size), cname, iRef);
        status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < values.length; ++index) {
            ncStatus = nc_insert_compound(ncId, typeid, fields[index], new NativeLong(index * 1), NC_BYTE);
            status(ncStatus);
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        for (byte value : values) {
            buffer.put(value);
        }
        buffer.rewind();
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), buffer);
        status(ncStatus);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, short[] values) {
        int ncStatus;
        IntByReference iRef = new IntByReference();
        int size = values.length * 2;
        ncStatus = nc_def_compound(ncId, new NativeLong(size), cname, iRef);
        status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < values.length; ++index) {
            ncStatus = nc_insert_compound(ncId, typeid, fields[index], new NativeLong(index * 2), NC_SHORT);
            status(ncStatus);
        }
        ShortBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asShortBuffer();
        for (short value : values) {
            buffer.put(value);
        }
        buffer.rewind();
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), buffer);
        status(ncStatus);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, int[] values) {
        int ncStatus;
        IntByReference iRef = new IntByReference();
        int size = values.length * 4;
        ncStatus = nc_def_compound(ncId, new NativeLong(size), cname, iRef);
        status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < values.length; ++index) {
            ncStatus = nc_insert_compound(ncId, typeid, fields[index], new NativeLong(index * 4), NC_INT);
            status(ncStatus);
        }
        IntBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asIntBuffer();
        for (int value : values) {
            buffer.put(value);
        }
        buffer.rewind();
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), buffer);
        status(ncStatus);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, long[] values) {
        int ncStatus;
        IntByReference iRef = new IntByReference();
        int size = values.length * 8;
        ncStatus = nc_def_compound(ncId, new NativeLong(size), cname, iRef);
        status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < values.length; ++index) {
            ncStatus = nc_insert_compound(ncId, typeid, fields[index], new NativeLong(index * 8), NC_INT64);
            status(ncStatus);
        }
        LongBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asLongBuffer();
        for (long value : values) {
            buffer.put(value);
        }
        buffer.rewind();
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), buffer);
        status(ncStatus);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, float[] values) {
        int ncStatus;
        IntByReference iRef = new IntByReference();
        int size = values.length * 4;
        ncStatus = nc_def_compound(ncId, new NativeLong(size), cname, iRef);
        status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < values.length; ++index) {
            ncStatus = nc_insert_compound(ncId, typeid, fields[index], new NativeLong(index * 4), NC_FLOAT);
            status(ncStatus);
        }
        FloatBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (float value : values) {
            buffer.put(value);
        }
        buffer.rewind();
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), buffer);
        status(ncStatus);
    }

    public static void generateCompoundAttributes(int ncId, int varId, String cname, String aname, String[] fields, double[] values) {
        int ncStatus;
        IntByReference iRef = new IntByReference();
        int size = values.length * 8;
        ncStatus = nc_def_compound(ncId, new NativeLong(size), cname, iRef);
        status(ncStatus);
        int typeid = iRef.getValue();
        for (int index = 0; index < values.length; ++index) {
            ncStatus = nc_insert_compound(ncId, typeid, fields[index], new NativeLong(index * 8), NC_DOUBLE);
            status(ncStatus);
        }
        DoubleBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asDoubleBuffer();
        for (double value : values) {
            buffer.put(value);
        }
        buffer.rewind();
        ncStatus = nc_put_att(ncId, varId, aname, typeid, new NativeLong(1), buffer);
        status(ncStatus);
    }

	enum XType {
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

		int xtype;
		int sizeBytes;

		XType (int xtype, int sizeBytes) {
			this.xtype = xtype;
			this.sizeBytes = sizeBytes;
		}

		public int getSizeBytes() {
			return sizeBytes;
		}

		public static XType findXType(int xType) {
			if (xType < 0 || xType > values().length) {
				throw new IllegalArgumentException("explain yourself jordan");
			} else {
				return values()[xType];
			}
		}
	}

}
