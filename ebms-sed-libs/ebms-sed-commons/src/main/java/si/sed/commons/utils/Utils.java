/*
* Copyright 2015, Supreme Court Republic of Slovenia 
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by 
* the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* https://joinup.ec.europa.eu/software/page/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis, WITHOUT 
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and  
* limitations under the Licence.
 */
package si.sed.commons.utils;

import java.util.Random;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class Utils {
    // States used in property parsing

    private static final int NORMAL = 0;
    private static final int SEEN_DOLLAR = 1;
    private static final int IN_BRACKET = 2;

    Random random = new Random();

    public static Utils mInstance = null;

    private Utils() {

    }

    public static synchronized Utils getInstance() {
        return mInstance = mInstance == null ? new Utils() : mInstance;
    }

    /**
     * Return a GUID as a string. This is completely arbitrary, and returns the
     * hexification of a random value followed by a timestamp.
     *
     * @return
     */
    public String getGuidString() {
        long rand = (random.nextLong() & 0x7FFFFFFFFFFFFFFFL)
                | 0x4000000000000000L;
        return Long.toString(rand, 32)
                + Long.toString(System.currentTimeMillis() & 0xFFFFFFFFFFFFFL, 32);
    }

    /*
    
    public static void addURLToSystemClassLoader(URL url) throws IntrospectionException {
        URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

        try {
            Method method = classLoaderClass.getDeclaredMethod("addURL", new Class[]{URL.class});
            method.setAccessible(true);
            method.invoke(systemClassLoader, new Object[]{url});
        } catch (Throwable t) {
            throw new IntrospectionException("Error when adding url "+url.getPath()+" to system ClassLoader ");
        }
    }*/
    public static boolean isEmptyString(String strVal) {
        return strVal == null || strVal.trim().isEmpty();
    }

    public static String getDomainFromAddress(String strVal) {
        if (isEmptyString(strVal)) {
            return "NO_DOMAIN";
        }
        if (strVal.contains("@")) {
            return strVal.substring(strVal.indexOf("@") + 1);
        }
        return strVal;

    }

    /**
     * Method is "borrowed" from org.jboss.util.StringPropertyReplacer; Go
     * through the input string and replace any occurance of ${p} with the
     * System.getProperty(p) value. If there is no such property p defined, then
     * the ${p} is replaced with "".
     *
     *
     * @param string - the string with possible ${} references     
     * @return the input string with all property references replaced if any. If
     * there are no valid references the input string will be returned.
     */
    public static String replaceProperties(final String string) {
        final char[] chars = string.toCharArray();
        StringBuilder buffer = new StringBuilder();
        boolean properties = false;
        int state = NORMAL;
        int start = 0;
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];

            // Dollar sign outside brackets
            if (c == '$' && state != IN_BRACKET) {
                state = SEEN_DOLLAR;
            } // Open bracket immediatley after dollar
            else if (c == '{' && state == SEEN_DOLLAR) {
                buffer.append(string.substring(start, i - 1));
                state = IN_BRACKET;
                start = i - 1;
            } // No open bracket after dollar
            else if (state == SEEN_DOLLAR) {
                state = NORMAL;
            } // Closed bracket after open bracket
            else if (c == '}' && state == IN_BRACKET) {
                // No content
                if (start + 2 == i) {
                    buffer.append("${}"); // REVIEW: Correct?
                } else // Collect the system property
                {
                    String value = null;

                    String key = string.substring(start + 2, i);
                    properties = true;
                    buffer.append(System.getProperty(key, ""));
                }
                start = i + 1;
                state = NORMAL;
            }
        }

        // No properties
        if (properties == false) {
            return string;
        }

        // Collect the trailing characters
        if (start != chars.length) {
            buffer.append(string.substring(start, chars.length));
        }
        // Done
        return buffer.toString();
    }

   
}
