/*
  * JBoss, Home of Professional Open Source
  *
  * Distributable under LGPL license.
  * See terms of license at gnu.org.
  */
package org.jboss.seam.ejb;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.ejb.AroundInvoke;
import javax.ejb.InvocationContext;
import javax.ejb.Remove;

import org.jboss.logging.Logger;
import org.jboss.seam.Finder;
import org.jboss.seam.Seam;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.BeginIf;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.EndIf;
import org.jboss.seam.contexts.Contexts;

/**
 * Interceptor for bijection and conversation scope management
 * for a session bean component
 * 
 * @author Gavin King
 * @version $Revision$
 */
public class SeamInterceptor
{

   private static final Logger log = Logger.getLogger(SeamInterceptor.class);

   @AroundInvoke
   public Object aroundInvoke(InvocationContext invocation) throws Exception
   {
      if ( Contexts.isProcessing() )
      {
         final Object bean = invocation.getBean();
         final Component seamComponent = getSeamComponent(bean);
   
         inject(bean, seamComponent);
   
         final Method method = invocation.getMethod();
         Object result;
         try
         {
            result = invocation.proceed();
         } 
         catch (Exception exception)
         {
            endConversationIfNecessary(method, exception);
            removeIfNecessary(bean, method, true, seamComponent);
            throw exception;
         }
         
         outject(bean, seamComponent);
         
         beginConversationIfNecessary(method, result);
         endConversationIfNecessary(method, result);
         
         removeIfNecessary(bean, method, false, seamComponent);
         
         return result;
      }
      else {
         return invocation.proceed();
      }
   }

   private void outject(final Object bean, final Component seamComponent)
   {
      if ( seamComponent.getOutFields().size()>0 || seamComponent.getOutMethods().size()>0 ) //only needed to hush the log message
      {
         log.info("injecting dependencies to: " + seamComponent.getName());
         seamComponent.outject(bean);
      }
   }

   private void inject(final Object bean, final Component seamComponent)
   {
      if ( seamComponent.getInFields().size()>0 || seamComponent.getInMethods().size()>0 ) //only needed to hush the log message
      {
         log.info("outjecting dependencies from: " + seamComponent.getName());
         seamComponent.inject(bean);
      }
   }

   private Component getSeamComponent(Object bean)
   {
      return new Finder().getComponent( Seam.getComponentName( bean.getClass() ) );
   }
   
   /**
    * If it was a @Remove method, also remove the component instance from the context
    */
   private void removeIfNecessary(Object bean, Method method, boolean exception, Component seamComponent)
   {
      boolean wasRemoved = method.isAnnotationPresent(Remove.class) &&
            ( !exception || !method.getAnnotation(Remove.class).retainIfException() );
      if ( wasRemoved )
      {
         seamComponent.getScope().getContext().remove( seamComponent.getName() );
         log.info("Stateful component was removed");
      }
   }

   private void beginConversationIfNecessary(Method method, Object result)
   {
      if ( method.isAnnotationPresent(Begin.class) )
      {
         Contexts.beginConversation();
      }
      else if ( method.isAnnotationPresent(BeginIf.class) )
      {
         String[] results = method.getAnnotation(BeginIf.class)
               .result();
         if (Arrays.asList(results).contains(result))
         {
            Contexts.beginConversation();
         }
      }
   }

   private void endConversationIfNecessary(Method method, Exception exception)
   {
      if (method.isAnnotationPresent(EndIf.class))
      {
         Class[] exceptions = method.getAnnotation(EndIf.class)
               .exception();
         if (Arrays.asList(exceptions).contains(exception.getClass()))
         {
            Contexts.endConversation();
         }
      }
   }

   private void endConversationIfNecessary(Method method, Object result)
   {
      if (method.isAnnotationPresent(End.class))
      {
         Contexts.endConversation();
      }
      else if (method.isAnnotationPresent(EndIf.class))
      {
         String[] results = method.getAnnotation(EndIf.class)
               .result();
         if (Arrays.asList(results).contains(result))
         {
            Contexts.endConversation();
         }
      }
   }

}
