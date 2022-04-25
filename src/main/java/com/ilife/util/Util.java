package com.ilife.util;

import java.util.UUID;

public class Util {
	public static String get32UUID() {
		String uuid = UUID.randomUUID().toString().trim().replaceAll("-", "");
		return uuid;
	}
	
	public static String md5(String source) {
		return md5(source.getBytes());
	}
	
	//取中间16位作为短码
	public static String md5_short(String source) {
		return md5(source.getBytes()).substring(8, 24);
	}
	
    public static String md5(byte[] source) {  
        String s = null;  
        char hexDigits[] = { // 用来将字节转换成 16 进制表示的字符  
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};  
        try {  
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");  
            md.update(source);  
            byte tmp[] = md.digest();          // MD5 的计算结果是一个 128 位的长整数，  
            // 用字节表示就是 16 个字节  
            char str[] = new char[16 * 2];   // 每个字节用 16 进制表示的话，使用两个字符，  
            // 所以表示成 16 进制需要 32 个字符  
            int k = 0;                                // 表示转换结果中对应的字符位置  
            for (int i = 0; i < 16; i++) {    // 从第一个字节开始，对 MD5 的每一个字节  
                // 转换成 16 进制字符的转换  
                byte byte0 = tmp[i];  // 取第 i 个字节  
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];  // 取字节中高 4 位的数字转换,  
                // >>> 为逻辑右移，将符号位一起右移  
                str[k++] = hexDigits[byte0 & 0xf];   // 取字节中低 4 位的数字转换  
            }  
            s = new String(str);  // 换后的结果转换为字符串  
   
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
        return s;  
    }
    
  //生成8位大小写随机码
    public static String get8bitCode() {
    	String get32uuid = get32UUID();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 4; i++) {
			sb.append(get32uuid.charAt((int) Math.round(Math.random() * 31)));
		}
		String KeyString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		sb.insert((int)Math.round(Math.random()*3), KeyString.charAt((int) Math.round(Math.random() * 25)));
		sb.insert((int)Math.round(Math.random()*4), KeyString.charAt((int) Math.round(Math.random() * 25)));
		String get32uuid2 = get32UUID();
		sb.insert((int)Math.round(Math.random()*5),get32uuid2.charAt((int) Math.round(Math.random() * 31)));
		sb.insert((int)Math.round(Math.random()*6),get32uuid2.charAt((int) Math.round(Math.random() * 31)));
		return sb.toString();
    }
    
    //生成6位大小写随机码
    public static String get6bitCode() {
    	String get32uuid = get32UUID();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 4; i++) {
			sb.append(get32uuid.charAt((int) Math.round(Math.random() * 31)));
		}
		String KeyString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		sb.insert((int)Math.round(Math.random()*3), KeyString.charAt((int) Math.round(Math.random() * 25)));
//		sb.insert((int)Math.round(Math.random()*4), KeyString.charAt((int) Math.round(Math.random() * 25)));
		String get32uuid2 = get32UUID();
		sb.insert((int)Math.round(Math.random()*5),get32uuid2.charAt((int) Math.round(Math.random() * 31)));
//		sb.insert((int)Math.round(Math.random()*6),get32uuid2.charAt((int) Math.round(Math.random() * 31)));
		return sb.toString();
    }
    
    //根据输入字符串生成8位大小写随机码：输入字符串相同则返回相同
    public static String get8bitCode(String seed) {
    	if(seed==null)
    		return get8bitCode();
    	String get32uuid = md5(seed);
		StringBuffer sb = new StringBuffer();
		int[] indexes = {0,1,8,9,16,17,24,25};//取固定位置字符
		for (int i = 0; i < indexes.length; i++) {
			sb.append(get32uuid.charAt(indexes[i]));
		}
		return sb.toString();
    }
    
    //根据输入字符串生成6位大小写随机码：输入字符串相同则返回相同
    public static String get6bitCode(String seed) {
    	if(seed==null)
    		return get6bitCode();
    	String get32uuid = md5(seed);
		StringBuffer sb = new StringBuffer();
		int[] indexes = {0,1,8,9,16,17};//取固定位置字符
		for (int i = 0; i < indexes.length; i++) {
			sb.append(get32uuid.charAt(indexes[i]));
		}
		return sb.toString();
    }
    
    public static String get3bitCode(String seed) {
    	int[] indexes = {0,1,2,3};//从6位短码中随机取3位，这里用于随机获取初始位置
    	String code6bit = get6bitCodeRandom(seed);
    	int r = (int)Math.random()*10;
    	int rStartIndex = indexes[r%4];
    	return code6bit.substring(rStartIndex);
    }
    
    public static String get6bitCodeRandom() {
    	return get6bitCodeRandom(get32UUID());
    }
    
	public static String get6bitCodeRandom(String seed){  
		 //生成短码所用字符数组
		 String[] chars = new String[]{ 
			  "a","b","c","d","e","f","g","h", 
			  "i","j","k","l","m","n","o","p", 
			  "q","r","s","t","u","v","w","x", 
			  "y","z","0","1","2","3","4","5", 
			  "6","7","8","9","A","B","C","D", 
			  "E","F","G","H","I","J","K","L", 
			  "M","N","O","P","Q","R","S","T", 
			  "U","V","W","X","Y","Z" 
		 }; 
		 //对传入网址进行MD5加密 
		 String hex = md5(seed); 
		 String[] shortCodes = new String[4]; 
		 for (int i = 0; i < 4; i++) 
		 { 
			  //把加密字符按照8位一组16进制与0x3FFFFFFF进行位与运算 
			  long hexLong = 0x3FFFFFFF & Long.parseLong( hex.substring(i * 8, i * 8+8), 16); 
			  String outChars = ""; 
			  for (int j = 0; j < 6; j++) 
			  { 
			   //把得到的值与0x0000003D进行位与运算，取得字符数组chars索引 
			   long index = 0x0000003D & hexLong; 
			   //把取得的字符相加 
			   outChars += chars[(int)index]; 
			   //每次循环按位右移5位 
			   hexLong = hexLong >> 5; 
			  } 
			  //把字符串存入对应索引的输出数组 
			  shortCodes[i] = outChars; 
			 } 
		 	 //int random = (int)System.currentTimeMillis()%4;
		 	 int random = seed.length()%4;//根据长度随机获取
			 return shortCodes[random]; 
		}
}
