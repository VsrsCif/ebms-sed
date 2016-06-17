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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class ReflectUtils {

    private static Map<Class, List<String>> BEAN_MEMBERS = new HashMap<>();

    // States used in property parsing
    public static List<String> getBeanMethods(Class cls) {
        if (BEAN_MEMBERS.containsKey(cls)) {
            return BEAN_MEMBERS.get(cls);
        }

        List<String> lst = new ArrayList<>();
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().startsWith("get") && m.getReturnType() != null
                    && (m.getParameterTypes() == null || m.getParameterTypes().length == 0)) {

                String name = m.getName().substring(3);
                if (lst.contains(name)) {
                    continue;
                }
                try {
                    Method m2 = cls.getDeclaredMethod("set" + name, m.getReturnType());
                    lst.add(name);
                } catch (NoSuchMethodException | SecurityException ignore) {

                }
            }

        }
        BEAN_MEMBERS.put(cls, lst);
        return lst;
    }

}
