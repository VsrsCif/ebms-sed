/*
 * Code was inspired by blog: Rafał Borowiec 
 * http://blog.codeleak.pl/2014/07/junit-testing-exception-with-java-8-and-lambda-expressions.html
 */
package si.jrc.msh.lmbd;

/**
 *
 * @author Joze Rihtarsic
 */
public class AssertionErrorBadInstanceThrown extends Error {

  public AssertionErrorBadInstanceThrown(Throwable cause) {
    super(cause.getMessage() + ""  + cause.getClass().toString(), cause);
  }
  
}
