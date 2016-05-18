/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.msh.ebms.inbox.mail.MSHInMail;

/**
 *
 * @author sluzba
 */
public class StringFormater {
    SimpleDateFormat msdf = new SimpleDateFormat("dd.MM.yyyy HH:mm.ss");
    
    
    public  String format(List<String> methods, Object obj, int i){
         
         Class cls = obj.getClass();
         StringWriter sw = new StringWriter();
         sw.write(i+".");
         
         for(String mth: methods){
         
             try {
                Method md =  cls.getDeclaredMethod("get"+mth);
                Object res  = md.invoke(obj, new Object[0]);
                
                String value = object2String(res);
                 sw.write(",");
                 sw.write(value.replace("\\", "\\\\").replace(",", "\\,"));
                
             } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                 Logger.getLogger(StringFormater.class.getName()).log(Level.SEVERE, null, ex);
             }
        }
         
       
        return sw.toString();
    }
    
    public  String format(String str, MSHInMail dce){
         HashMap<String, Object> hm= new HashMap<>();
         
         
         Method[] mthLst = dce.getClass().getDeclaredMethods();
         for (Method mt:mthLst ){
             
             if (mt.getName().startsWith("get")){
                 String strName = mt.getName().substring(3);
                 Object put = null;
                 try {
                     if ( mt.getParameterTypes() == null ||  mt.getParameterTypes().length ==0) {                     
                        put = mt.invoke(dce, new Object[0]);
                     }
                 } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                     Logger.getLogger(StringFormater.class.getName()).log(Level.SEVERE, null, ex);
                 }
                 hm.put(strName, put);
                  
             }
         }
        return format(str, hm);
    }
    
    
    
     public String format(String str, Map<String, Object> values) {

        StringBuilder builder = new StringBuilder(str);

        for (Map.Entry<String, Object> entry : values.entrySet()) {

            int start;
            String pattern = "${" + entry.getKey() + "}";
            String value = object2String(entry.getValue());

            // Replace every occurence of %(key) with value
            while ((start = builder.indexOf(pattern)) != -1) {
                builder.replace(start, start + pattern.length(), value);
            }
        }

        return builder.toString();
    }
     
     
     private String object2String(Object o){
         String res = null; 
         if (o == null){
             res = "";
        } else if (o instanceof String){
            res = (String)o;
        } else if (o instanceof Integer){
            res = ((Integer)o).toString();
        } else if (o instanceof BigInteger){
            res = ((BigInteger)o).toString();
        } else if (o instanceof BigDecimal){
            res = ((BigDecimal)o).toString();
        } else if (o instanceof Double){
            res = ((Double)o).toString();
        } else if (o instanceof Date){
            res = msdf.format((Date)o);
        } else {
            res = o.toString();
        }
         return res;
         
     }
}
