package uchicago.src.reflector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ByteArgInvoker extends Invoker {

  Byte value = null;

  public ByteArgInvoker(Object o, Method m, String param) {
    super(o, m, param);
  }

  @Override
  protected void check() throws InvokerException {
    try {
    	Double d = Double.valueOf(param);
    	value = new Byte(d.byteValue());
    } catch (NumberFormatException ex) {
      throw new InvokerException("Invalid Parameter: byte or Byte expected");
    }
  }

  @Override
  protected void invoke() throws InvocationTargetException, IllegalAccessException
  {
    method.invoke(object, new Object[] {value});
  }
}
