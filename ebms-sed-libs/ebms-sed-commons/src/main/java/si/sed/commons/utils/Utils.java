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

}
