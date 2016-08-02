/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import si.sed.commons.utils.entity.TestReflectClass;

/**
 *
 * @author sluzba
 */
public class ReflectUtilsTest {
  


  /**
   * Test of getBeanMethods method, of class ReflectUtils.
   */
  @Test
  public void testGetBeanMethods() {
    List<String> lst =  ReflectUtils.getBeanMethods(TestReflectClass.class);
    assertEquals("Test class have 3 bean methods", 3, lst.size());
    assertTrue("Test class have Age bean method", lst.contains("Age"));
    assertTrue("Test class have Name bean method", lst.contains("Name"));
    assertTrue("Test class have Height bean method", lst.contains("Height"));    
  }
  
}
