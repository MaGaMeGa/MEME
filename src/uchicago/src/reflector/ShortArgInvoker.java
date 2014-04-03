package uchicago.src.reflector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ShortArgInvoker extends Invoker {

  Short value = null;

  public ShortArgInvoker(Object o, Method m, String param) {
    super(o, m, param);
  }

  @Override
  protected void check() throws InvokerException {
    try {
    	Double d = Double.valueOf(param);
    	value = new Short(d.shortValue());
    } catch (NumberFormatException ex) {
      throw new InvokerException("Invalid Parameter: short or Short expected");
    }
  }

  @Override
  protected void invoke() throws InvocationTargetException, IllegalAccessException
  {
    method.invoke(object, new Object[] {value});
  }
}
