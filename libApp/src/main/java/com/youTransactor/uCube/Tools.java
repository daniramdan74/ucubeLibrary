/**
 * Copyright (C) 2011-2016, YouTransactor. All Rights Reserved.
 *
 * Use of this product is contingent on the existence of an executed license
 * agreement between YouTransactor or one of its sublicensee, and your
 * organization, which specifies this software's terms of use. This software
 * is here defined as YouTransactor Intellectual Property for the purposes
 * of determining terms of use as defined within the license agreement.
 */

package com.youTransactor.uCube;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author gbillard on 3/10/16.
 */
public class Tools {

	private Tools() {}

	public static String parseVersion(byte[] data) {
		StringBuffer buffer = new StringBuffer();

		if (data != null) {
			for (int i = 0; i < data.length; i++) {
				if (buffer.length() > 0) {
					buffer.append('.');
				}

				buffer.append(String.valueOf(data[i]));
			}
		}

		return buffer.toString();
	}

	/**
	 * this function transforms the Partnumber from byte[] to String after deleting the first 3 spaces unused
	 *
	 * @param buffer the response of GetInfo
	 * @return String format decimal or "" if error
	 */
	public static String parsePartNumber(byte[] buffer) {
		if(buffer != null && buffer.length == SVPP_PART_NUMBER_LEN){
			byte [] realPartNumber = new byte [SVPP_PART_NUMBER_LEN - SVPP_PART_NUMBER_OFFSET_OF_USEFULL_DATA];

			try {
				System.arraycopy(buffer, SVPP_PART_NUMBER_OFFSET_OF_USEFULL_DATA, realPartNumber, 0, SVPP_PART_NUMBER_LEN - SVPP_PART_NUMBER_OFFSET_OF_USEFULL_DATA);
				return new String(realPartNumber, "UTF-8");

			} catch (UnsupportedEncodingException ignored) {}
		}

		return "";
	}

	/**
	 * this function transform the value of serial number from byte[] to proprietary Format added
	 * to the url in both methods(GET,POST), first transformation is to int [] then add the int after
	 * multiply it with 256 power its index.
	 *
	 *example: serial number :00 F1 5C AD 88
	 * hexString  , Decimal * Factor
	 *  (00)           = 00*256^4
	 *  (F1)           = 241*256^3
	 *  (5C)           = 92*256^2
	 *  @Math.pow function to make the power 5^6 Math.pow(5,6)
	 *
	 *@reference check the aspect page 55 @IDENT.SetInfoProd(02)
	 *
	 *
	 * @param bytes
	 * @return "" if error
	 */
	public static String parseSerial(byte[] bytes) {
		if (bytes == null || bytes.length != SVPP_SERIAL_NUMBER_LEN) {
			return "";
		}

		int i = 0;
		int m = 256;
		long decimalong = 0;

		/* creation of int array to transform the bytes to int array in format decimal */
		int[] iarray = new int[bytes.length];

		for (byte b : bytes) {
			iarray[i++] = b & 0xFF;
		}

		/* add the int after multiply it with 256 power its index */

		for (int p = 1; p <= SVPP_SERIAL_NUMBER_LEN; ){
			/* adding all the result to decimallong */
			decimalong += iarray [(SVPP_SERIAL_NUMBER_LEN - p)] * Math.pow(m , (p-1)) ;
			p++;
		}

		/* divide the decimallong /4 */
		return String.valueOf(decimalong /4);
	}

	/**
	 * makeUnsignedShort
	 * To avoid sign issue when adding two bytes
	 */
    public static short makeShort(byte MSB, byte LSB ){
		return (short) (((MSB & 0xFF) * 0x100) + (LSB & 0xFF));
	}

	/**
	 * function of transform the byteArray to Hexadecimal String
	 * @param bytes wanted to be transform
	 * @return String of HexString
	 */
	public static String bytesToHex(byte[] bytes) {
		return bytes == null || bytes.length == 0 ? "" : new String(Hex.encodeHex(bytes)).toUpperCase();
	}

	public static byte[] toBCD_double(double value, int length) {
		NumberFormat formatter = new DecimalFormat("#");
		String val = formatter.format(value);

		return hexStringToByteArray(StringUtils.leftPad(val, length * 2 , '0'));
	}

	public static double fromBCD_double(byte[] buff) {
		String val = new String(Hex.encodeHex(buff));
		return Double.valueOf(val);
	}

	public static byte[] toBCD(int value, int length) {
		byte[] res = new byte[length];

		for (int i = length - 1; i >= 0; i--) {
			res[i] = (byte) (value % 10);
			value = value / 10;
			res[i] |= (byte) ((value % 10) << 4);
			value = value / 10;
		}

		return res;
	}

	public static int fromBCD(byte[] buff) {
		int res = 0;

		if (buff != null) {
			for (int i = 0; i < buff.length; i++) {
				res *= 10;
				res += ((buff[i] >> 4) & 0xF);
				res *= 10;
				res += (buff[i] & 0xF);
			}
		}

		return res;
	}

	public static byte[] intToByteArray(int value, int size) {
		byte[] res = new byte[size];

		for (int i = size - 1; i >= 0; i--) {
			res[i] = (byte) value;
			value = value >> 8;
		}

		return res;
	}

	private static final short SVPP_VERSION_LEN = 4;
	private static final short SVPP_PART_NUMBER_LEN = 15;
	private static final short SVPP_PART_NUMBER_OFFSET_OF_USEFULL_DATA = 3;
	private static final short SVPP_SERIAL_NUMBER_LEN = 5;



	/**
	 * this function is to convert the Version from string od decimal in format (xxx.xxx.xxx.xxx) to byteArray
	 * with the length 4 bytes every byte has a value in hexadecimal in format(AB), The maximum value is (FF)=255
	 * and minimum (00)=0.
	 * @param version : the String of version
	 * @return Byte[] of the version
	 * example : (1.10.100.10) =>  {1,A,64,A}
	 */

	public static byte[] stringDecimalVersionToHexByteArray(String version) {

		byte versionByteArray[] = new byte[SVPP_VERSION_LEN];

		// look up until dot (.)
		// then put it into byte array
		int offset = 0;
		int i;
		int index;
		String hexStringTemp;
		String intStringTemp;

		// Check string length 0.0.0.0 => 255.255.255.255
		// Count the number of dots

		for ( i = 0 ; i < versionByteArray.length; i++) {

			index = version.indexOf(".", offset);

			// If dot is not found that means we inspect the last element (xxx)
			if(index != -1){
				intStringTemp = version.substring(offset, index);
				offset = index +1;
			} else {
				intStringTemp = version.substring(offset);
			}

			hexStringTemp = Integer.toHexString(Integer.parseInt(intStringTemp, 16));
			versionByteArray[i] = Byte.parseByte(hexStringTemp);
		}

		return versionByteArray;
	}


    public static byte[] hexStringToByteArray(String s) {
		try {
			return Hex.decodeHex(s.toCharArray());
		} catch (DecoderException e) {
			return new byte[0];
		}
    }

    public static int neg_byte_to_int(byte b) {
        return ((b >= 0) ? b : b + 0x100);
    }

    public static byte intToBcdByte(int i) {
        byte lsb;
        byte msb;

        if (i > 99)
            return (byte) 0xFF;

        lsb = (byte) (i % 10);
        msb = (byte) (i / 10);

        return (byte) (msb * 0x10 + lsb);
    }

}
